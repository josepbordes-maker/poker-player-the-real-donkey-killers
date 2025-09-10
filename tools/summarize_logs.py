#!/usr/bin/env python3
import sys, json, re

"""
Tiny log summarizer.
Reads Lean Poker platform JSON array from stdin and prints a compact table of our actions and outcomes.

Usage:
  cat snippet.json | tools/summarize_logs.py "The Real Donkey Killers"

Outputs TSV: round	street	action	amount	pot	buyin	message
"""

def street_from_state(state):
    cc = state.get("community_cards", [])
    n = len(cc)
    return {0: "PRE", 3: "FLOP", 4: "TURN", 5: "RIVER"}.get(n, str(n))

def main():
    team = sys.argv[1] if len(sys.argv) > 1 else "The Real Donkey Killers"
    data = sys.stdin.read().strip()
    try:
        events = json.loads(data)
    except Exception:
        # maybe JSON lines; try to wrap
        events = [json.loads(line) for line in data.splitlines() if line.strip()]

    print("round\tstreet\taction\tamount\tpot\tbuyin\tmessage")
    for ev in events:
        t = ev.get("type", "")
        gs = ev.get("game_state", {})
        msg = ev.get("message", "")
        if t == "bet" and team in msg:
            rnd = gs.get("round", "")
            st = street_from_state(gs)
            m = re.search(r"bet of (\d+)|bet of 0 \((\w+)\)", msg)
            amt = "0"
            if m:
                amt = m.group(1) or "0"
            act = "call" if "(call)" in msg else "raise" if "(raise)" in msg else "check" if "(check)" in msg else ("bet" if amt != "0" else "fold" if "(fold)" in msg else "?")
            pot = gs.get("pot", "")
            buyin = gs.get("current_buy_in", "")
            print(f"{rnd}\t{st}\t{act}\t{amt}\t{pot}\t{buyin}\t{msg}")
        elif t == "winner_announcement" and team in msg:
            rnd = gs.get("round", "")
            st = street_from_state(gs)
            print(f"{rnd}\t{st}\twon\t\t{gs.get('pot','')}\t{gs.get('current_buy_in','')}\t{msg}")

if __name__ == "__main__":
    main()

