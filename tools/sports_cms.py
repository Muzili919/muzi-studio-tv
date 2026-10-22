#!/usr/bin/env python3
"""Runnable entry for the sports aggregator. Delegates to deploy/server/sports_cms.py."""
import pathlib
import runpy

TARGET = pathlib.Path(__file__).resolve().parents[1] / "deploy" / "server" / "sports_cms.py"
runpy.run_path(str(TARGET), run_name="__main__")
