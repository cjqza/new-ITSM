#!/usr/bin/env python3
"""
Build RAG knowledge index from documents in the knowledge directory.

Usage:
    python rag_build.py [--doc-dir /path/to/docs]
"""

import sys
import os
import logging
import argparse

# Add parent dir to path
sys.path.insert(0, os.path.dirname(__file__))

from rag_core import rebuild_index, KNOWLEDGE_DIR

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)


def main():
    parser = argparse.ArgumentParser(description="Build RAG knowledge index")
    parser.add_argument("--doc-dir", default=KNOWLEDGE_DIR, help="Directory containing knowledge documents")
    args = parser.parse_args()

    if not os.path.exists(args.doc_dir):
        logger.error("Document directory does not exist: %s", args.doc_dir)
        logger.info("Please create the directory and add .md or .txt files to it.")
        sys.exit(1)

    logger.info("Building index from: %s", args.doc_dir)
    index = rebuild_index(args.doc_dir)
    
    if index.index and index.index.ntotal > 0:
        logger.info("Done! Index has %d vectors from %d chunks.", index.index.ntotal, len(index.chunks))
    else:
        logger.warning("No vectors were indexed. Check that the document directory contains .md or .txt files.")


if __name__ == "__main__":
    main()
