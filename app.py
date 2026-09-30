from __future__ import annotations

import hashlib
from pathlib import Path
import tempfile

import numpy as np
import streamlit as st

from core.documents import DocumentChunk, DocumentProcessingError, chunk_document, extract_document
from core.embeddings import DEFAULT_EMBEDDING_MODEL, embed_texts, load_embedding_model
from core.ollama_client import OllamaStatus, detect_ollama_models
from core.rag_pipeline import RAGAnswer, answer_question


st.set_page_config(
    page_title="Fieldnotes | Local RAG Workshop",
    page_icon=":material/menu_book:",
    layout="wide",
    initial_sidebar_state="expanded",
)

st.markdown(
    """
    <style>
    :root { --ink: #202923; --muted: #66736b; --paper: #f5f7f2; --line: #dce3d9;
            --green: #245c45; --mint: #e4f0e8; --amber: #f2ead7; --red: #f5e5e2; }
        html, body, [class*="css"] { font-family: 'Bahnschrift', 'Aptos', sans-serif; color: var(--ink); }
    .stApp { background: radial-gradient(ellipse at 88% 0%, #e9f1e8 0, transparent 34%), var(--paper); }
    h1, h2, h3 { font-family: 'Manrope', sans-serif !important; letter-spacing: 0 !important; }
    h1 { font-size: 2.1rem !important; font-weight: 800 !important; }
    [data-testid="stSidebar"] { background: #eef2ec; border-right: 1px solid var(--line); }
    [data-testid="stMetric"] { background: rgba(255,255,255,.68); padding: 14px 16px; border: 1px solid var(--line); border-radius: 6px; }
    [data-testid="stFileUploader"] section { background: rgba(255,255,255,.72); border: 1px dashed #a9b8aa; border-radius: 6px; }
    .eyebrow { color: var(--green); text-transform: uppercase; font-size: .72rem; font-weight: 700; letter-spacing: .12em; }
    .lede { color: var(--muted); margin-top: -.7rem; }
    .status-line { color: var(--muted); font-size: .88rem; }
    .flow { padding: 13px 16px; background: rgba(255,255,255,.64); border: 1px solid var(--line); border-left: 3px solid var(--green); border-radius: 4px; color: #3d4a41; font-size: .88rem; }
    .stButton > button, .stDownloadButton > button { border-radius: 4px; }
    [data-testid="stChatMessage"] { border: 1px solid var(--line); border-radius: 6px; background: rgba(255,255,255,.66); }
    @media (max-width: 700px) { h1 { font-size: 1.75rem !important; } }
    </style>
    """,
    unsafe_allow_html=True,
)


@st.cache_resource(show_spinner=False)
def get_embedding_model():
    return load_embedding_model()


@st.cache_data(show_spinner=False)
def extract_and_chunk(file_bytes: bytes, filename: str, chunk_size: int, overlap: int):
    suffix = Path(filename).suffix.lower()
    temporary_path = None
    try:
        with tempfile.NamedTemporaryFile(suffix=suffix, delete=False) as temporary_file:
            temporary_file.write(file_bytes)
            temporary_path = Path(temporary_file.name)
        pages = extract_document(temporary_path)
        chunks = chunk_document(pages, filename, chunk_size, overlap)
        return pages, chunks
    except DocumentProcessingError:
        raise
    except Exception as exc:
        raise DocumentProcessingError(f"Could not process {filename}: {exc}") from exc
    finally:
        if temporary_path and temporary_path.exists():
            temporary_path.unlink()


@st.cache_data(ttl=8, show_spinner=False)
def get_ollama_status() -> OllamaStatus:
    return detect_ollama_models()


def page_count(chunks: list[DocumentChunk]) -> str:
    pages = {chunk.page_number for chunk in chunks if chunk.page_number is not None}
    return str(len(pages)) if pages else "DOCX"


for key, initial in {
    "chunks": [],
    "embeddings": np.empty((0, 0), dtype=np.float32),
    "indexed_files": [],
    "conversation": [],
    "index_signature": None,
}.items():
    if key not in st.session_state:
        st.session_state[key] = initial


with st.sidebar:
    st.markdown('<div class="eyebrow">Workshop controls</div>', unsafe_allow_html=True)
    st.header("Retrieval settings")
    top_k = st.slider("Top-K chunks", min_value=1, max_value=8, value=4)
    min_similarity = st.slider(
        "Minimum cosine similarity", min_value=0.0, max_value=0.8, value=0.18, step=0.02,
        help="Chunks below this score are not sent to the language model.",
    )
    chunk_size = st.slider("Chunk size (characters)", 300, 1800, 900, step=100)
    overlap = st.slider("Chunk overlap", 0, min(400, chunk_size - 1), min(120, chunk_size - 1), step=20)
    temperature = st.slider("Answer temperature", 0.0, 1.0, 0.2, step=0.1)
    st.caption(f"Embedding model · `{DEFAULT_EMBEDDING_MODEL}` · CPU. First use downloads weights to the local cache.")
    st.divider()
    st.markdown("**Local model**")
    if st.button("Refresh Ollama status", icon=":material/refresh:", use_container_width=True):
        get_ollama_status.clear()
        st.rerun()
    ollama = get_ollama_status()
    if ollama.connected:
        if ollama.selection.name:
            family = ollama.selection.family
            st.success(f"{family} · `{ollama.selection.name}`", icon=":material/check_circle:")
        else:
            st.warning("Connected, but no Qwen or Llama model is installed.", icon=":material/warning:")
    else:
        st.error("Ollama is not responding at 127.0.0.1:11434.", icon=":material/error:")
        st.caption("Start the Ollama app or run `ollama serve`.")


st.markdown('<div class="eyebrow">Local document intelligence</div>', unsafe_allow_html=True)
st.title("Fieldnotes")
st.markdown(
    '<p class="lede">Chat with your documents. Every answer starts with visible retrieval, '
    'runs locally, and points back to its source.</p>',
    unsafe_allow_html=True,
)

if ollama.selection.name:
    st.markdown(
        f'<div class="flow">● Ollama connected &nbsp; / &nbsp; '
        f'<strong>{ollama.selection.family}</strong> selected automatically &nbsp; / &nbsp; '
        f'<code>{ollama.selection.name}</code></div>',
        unsafe_allow_html=True,
    )
elif ollama.connected:
    st.markdown(
        '<div class="flow">Ollama connected. Install a local Qwen or Llama model to enable answers.</div>',
        unsafe_allow_html=True,
    )
else:
    st.markdown('<div class="flow">Ollama is offline. You can still upload, index, and inspect retrieval.</div>', unsafe_allow_html=True)

st.subheader("1 · Build your knowledge base")
uploaded_files = st.file_uploader(
    "Choose PDF or DOCX documents",
    type=["pdf", "docx"],
    accept_multiple_files=True,
    help="Text-based PDFs and DOCX files are supported. Image-only PDFs need OCR first.",
)

upload_signature = hashlib.sha256(
    b"".join(file.name.encode("utf-8") + file.getvalue() for file in (uploaded_files or []))
).hexdigest()
settings_signature = (chunk_size, overlap)
if st.button("Index documents", type="primary", disabled=not uploaded_files, icon=":material/search:"):
    collected_chunks: list[DocumentChunk] = []
    indexed_files = []
    errors = []
    progress = st.progress(0, text="Preparing documents…")
    for index, file in enumerate(uploaded_files):
        try:
            with st.spinner(f"Extracting and chunking {file.name}…"):
                pages, file_chunks = extract_and_chunk(
                    file.getvalue(), file.name, chunk_size, overlap
                )
            collected_chunks.extend(file_chunks)
            indexed_files.append(
                {
                    "name": file.name,
                    "size": file.size,
                    "characters": sum(len(page.text) for page in pages),
                    "pages": page_count(file_chunks),
                    "chunks": len(file_chunks),
                }
            )
        except Exception as exc:
            errors.append(f"{file.name}: {exc}")
        progress.progress((index + 1) / len(uploaded_files), text=f"Read {file.name}")

    if collected_chunks:
        try:
            with st.spinner("Loading the local embedding model and creating vectors…"):
                embedding_model = get_embedding_model()
                vectors = embed_texts(embedding_model, [chunk.text for chunk in collected_chunks])
            st.session_state.chunks = collected_chunks
            st.session_state.embeddings = vectors
            st.session_state.indexed_files = indexed_files
            st.session_state.index_signature = (upload_signature, settings_signature)
            st.session_state.conversation = []
            st.success(f"Indexed {len(indexed_files)} document(s) into {len(collected_chunks)} chunks.")
        except Exception as exc:
            st.error(f"Embedding setup failed: {exc}. Check your internet connection for the first model download, then retry.")
            st.session_state.chunks = []
            st.session_state.embeddings = np.empty((0, 0), dtype=np.float32)
            st.session_state.indexed_files = []
    for error in errors:
        st.warning(error)
    progress.empty()


indexed = bool(st.session_state.chunks)
index_matches_selection = (
    not uploaded_files
    or st.session_state.index_signature == (upload_signature, settings_signature)
)
index_is_current = indexed and index_matches_selection
metrics = st.columns(4)
metrics[0].metric("Documents", len(st.session_state.indexed_files))
metrics[1].metric("Chunks", len(st.session_state.chunks))
metrics[2].metric("Embeddings", "Ready" if indexed else "Not indexed")
metrics[3].metric("Retrieval", "Ready" if indexed else "Waiting")

if st.session_state.indexed_files:
    with st.expander("Indexed documents", expanded=True):
        for item in st.session_state.indexed_files:
            st.markdown(
                f"**{item['name']}** · {item['size'] / 1024:.0f} KB · "
                f"{item['characters']:,} characters · {item['pages']} pages · "
                f"{item['chunks']} chunks"
            )
if uploaded_files and st.session_state.index_signature != (upload_signature, settings_signature):
    st.info("Upload selection or chunk settings changed. Re-index to update the knowledge base.")

st.subheader("2 · Ask your documents")
if not index_is_current:
    st.caption("Index at least one document to start a conversation.")
if st.button("Clear chat", icon=":material/delete:", disabled=not st.session_state.conversation):
    st.session_state.conversation = []
    st.rerun()

for message in st.session_state.conversation:
    with st.chat_message(message["role"]):
        st.markdown(message["content"])
        if message.get("retrieved"):
            with st.expander(f"Retrieved sources · {len(message['retrieved'])} chunks"):
                for result in message["retrieved"]:
                    chunk = result.chunk
                    page = f" · page {chunk.page_number}" if chunk.page_number is not None else ""
                    st.markdown(
                        f"**{chunk.chunk_id}** · {chunk.source}{page} · "
                        f"cosine similarity `{result.score:.3f}`"
                    )
                    st.caption(chunk.text)

question = st.chat_input("Ask a question about the indexed documents…", disabled=not index_is_current)
if question:
    st.session_state.conversation.append({"role": "user", "content": question})
    with st.chat_message("user"):
        st.markdown(question)
    try:
        with st.chat_message("assistant"):
            with st.spinner("Comparing the question with your document chunks…"):
                embedding_model = get_embedding_model()
                response: RAGAnswer = answer_question(
                    question,
                    st.session_state.chunks,
                    st.session_state.embeddings,
                    embedding_model,
                    ollama.selection.name,
                    top_k,
                    min_similarity,
                    temperature,
                )
            st.markdown(response.answer)
            if response.retrieved:
                with st.expander(f"Retrieved sources · {len(response.retrieved)} chunks"):
                    for result in response.retrieved:
                        chunk = result.chunk
                        page = f" · page {chunk.page_number}" if chunk.page_number is not None else ""
                        st.markdown(
                            f"**{chunk.chunk_id}** · {chunk.source}{page} · "
                            f"cosine similarity `{result.score:.3f}`"
                        )
                        st.caption(chunk.text)
        st.session_state.conversation.append(
            {"role": "assistant", "content": response.answer, "retrieved": response.retrieved}
        )
    except Exception as exc:
        friendly = f"The question could not be answered: {exc}"
        with st.chat_message("assistant"):
            st.error(friendly)
        st.session_state.conversation.append({"role": "assistant", "content": friendly})

with st.expander("How this RAG demo works"):
    st.markdown(
        "**Document → Extract → Chunk → Embed → Compare → Retrieve → Context → LLM → Answer**\n\n"
        "Embeddings are vectors that represent semantic meaning. Cosine similarity compares their "
        "directions: $\\cos(\\theta)=\\frac{A\\cdot B}{\\|A\\|\\|B\\|}$. Retrieval chooses the "
        "closest chunks; only those excerpts are supplied to the local LLM. The model does not "
        "automatically know your upload. Uploaded text is untrusted reference data, never instructions."
    )