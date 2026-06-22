from pyvis.network import Network
import re
import colorsys
import pandas as pd

def _rgb_to_hex(rgb_color):
    r, g, b = [int(c * 255) for c in rgb_color]
    return f"#{r:02x}{g:02x}{b:02x}"


def generate_distinct_colors(n):
    colors = []
    for i in range(n):
        hue = i / (n if n > 0 else 1)
        rgb = colorsys.hls_to_rgb(hue, 0.5, 0.7)
        colors.append(_rgb_to_hex(rgb))
    return colors


def _parse_flow_input(flow_input):
    """Parses list or DataFrame into a standard list of dicts."""
    flow_records = []

    if flow_input is None:
        return []

    if isinstance(flow_input, pd.DataFrame):
        # Filter for active flows if 'level' column exists
        if "level" in flow_input.columns:
            active_flows = flow_input[flow_input["level"] > 0.5]
        else:
            active_flows = flow_input

        for _, row in active_flows.iterrows():
            # Adjust indices based on GAMS domain order (v, t, v, t, o)
            flow_records.append(
                {
                    "u": row.iloc[0],
                    "t_u": row.iloc[1],
                    "v": row.iloc[2],
                    "t_v": row.iloc[3],
                    "order": row.iloc[4],
                    "value": 1,
                }
            )
    elif isinstance(flow_input, list):
        flow_records = flow_input

    # Normalize time steps
    for f in flow_records:
        f["t_u"] = int(str(f["t_u"]).replace("t", ""))
        f["t_v"] = int(str(f["t_v"]).replace("t", ""))

    return flow_records


def _get_node_info(node_name, time_step):
    match = re.search(r"v_([a-zA-Z]+)(\d*)", str(node_name))
    if match:
        style_key = match.group(1)
        node_index = int(match.group(2)) if match.group(2) else 0
        label = f"{style_key}{node_index}\nt{time_step}"
        if style_key == "source":
            label = "Source"
        if style_key == "magic":
            label = "Slack"
        return style_key, node_index, int(time_step), label
    return "unknown", 0, int(time_step), str(node_name)


# --- Main Plotting Function (Single Solution) ---
def plot_mcf(filename="mcf_plot.html", flow_input=None, demand_records=None):
    """Plots a single solution with detailed demand info and hover quantities."""
    if flow_input is None:
        flow_input = []
    if demand_records is None and isinstance(flow_input, pd.DataFrame):
        demand_records = pd.DataFrame()

    flow_records = _parse_flow_input(flow_input)
    _generate_network(filename, flow_records, demand_records, mode="single")


# --- Comparison Function (Classical vs QUBO) ---
def plot_comparison(
        filename="mcf_comparison.html",
        classical_input=None,
        qubo_input=None,
        demand_records=None,
):
    """Plots a comparison with detailed demand info and hover quantities."""
    if classical_input is None:
        classical_input = []
    if qubo_input is None:
        qubo_input = []

    c_records = _parse_flow_input(classical_input)
    d_records = _parse_flow_input(qubo_input)

    c_keys = set((r["u"], r["t_u"], r["v"], r["t_v"], r["order"]) for r in c_records)
    d_keys = set((r["u"], r["t_u"], r["v"], r["t_v"], r["order"]) for r in d_records)

    combined_flows = []

    # Matches (Green)
    for k in c_keys.intersection(d_keys):
        combined_flows.append(
            {
                "u": k[0],
                "t_u": k[1],
                "v": k[2],
                "t_v": k[3],
                "order": k[4],
                "color": "green",
                "status": "Match",
                "width": 4,
            }
        )

    # Classical Only (Blue)
    for k in c_keys - d_keys:
        combined_flows.append(
            {
                "u": k[0],
                "t_u": k[1],
                "v": k[2],
                "t_v": k[3],
                "order": k[4],
                "color": "blue",
                "status": "Classical Only",
                "width": 2,
            }
        )

    # QUBO Only (Red)
    for k in d_keys - c_keys:
        combined_flows.append(
            {
                "u": k[0],
                "t_u": k[1],
                "v": k[2],
                "t_v": k[3],
                "order": k[4],
                "color": "red",
                "status": "QUBO Only",
                "width": 2,
            }
        )

    print(
        f"Comparison Stats: Matches={len(c_keys.intersection(d_keys))}, Classical_Only={len(c_keys-d_keys)}, QUBO_Only={len(d_keys-c_keys)}"
    )

    _generate_network(filename, combined_flows, demand_records, mode="compare")


# --- Internal Graph Generation Logic ---
def _generate_network(filename, flow_records, demand_records, mode="single"):
    NODE_STYLES = {
        "source": {"label": "Source", "level": 0},
        "fac": {"label": "Factory", "level": 1},
        "sort": {"label": "Sorting", "level": 2},
        "db": {"label": "DieBank", "level": 3},
        "asm": {"label": "Assembly", "level": 4},
        "test": {"label": "Testing", "level": 5},
        "dc": {"label": "Dist. Ctr", "level": 6},
        "magic": {"label": "Slack", "level": 7},
    }

    Y_TIME_SPACING, Y_UNIT_SPACING, X_SPACING = 120, 50, 250
    UNIT_COLORS = ["#ADD8E6", "#87CEEB", "#6495ED", "#4169E1", "#000080"]

    net = Network(height="100vh", width="100%", notebook=True, directed=True)
    added_nodes = set()
    edge_counts = {}
    max_time_step = 0

    # --- 1. Map Orders to Quantities ---
    # We sum up the values for each order to get total flow quantity
    order_qty_map = {}
    if demand_records is not None and not demand_records.empty:
        for _, row in demand_records.iterrows():
            # Robustly get 'o' and 'value'
            o_val = row.get("o", row.iloc[0])
            val = row.get("value", row.iloc[-1])  # Value is typically last column

            # Aggregate if multiple entries per order (e.g. multiple products)
            current_qty = order_qty_map.get(o_val, 0)
            try:
                current_qty += float(val)
            except (ValueError, TypeError):
                pass
            order_qty_map[o_val] = current_qty

    # --- 2. Setup Colors ---
    order_color_map = {}
    if mode == "single":
        unique_orders = sorted(list(set(f["order"] for f in flow_records)))
        order_colors = generate_distinct_colors(len(unique_orders))
        order_color_map = {o: c for o, c in zip(unique_orders, order_colors)}

    # --- 3. Build Graph ---
    for flow in flow_records:
        u, tu, v, tv = flow["u"], flow["t_u"], flow["v"], flow["t_v"]
        order = flow["order"]
        max_time_step = max(max_time_step, tu, tv)

        source_id, target_id = f"{u},{tu}", f"{v},{tv}"

        # Add Nodes
        for node_name, time, n_id in [(u, tu, source_id), (v, tv, target_id)]:
            if n_id not in added_nodes:
                key, idx, t, lbl = _get_node_info(node_name, time)
                style = NODE_STYLES.get(key, {"level": 8})
                y_pos = (t * Y_TIME_SPACING) + (idx * Y_UNIT_SPACING)
                if key == "source":
                    y_pos = 50

                node_col = (
                    UNIT_COLORS[idx % len(UNIT_COLORS)]
                    if key != "source"
                    else "#FFD700"
                )
                if key == "magic":
                    node_col = "#A9A9A9"

                net.add_node(
                    n_id,
                    label=lbl,
                    level=style["level"],
                    x=style["level"] * X_SPACING,
                    y=y_pos,
                    color=node_col,
                    shape="circle",
                    physics=False,
                )
                added_nodes.add(n_id)

        # Edge Logic
        pair_key = tuple(sorted((source_id, target_id)))
        edge_counts[pair_key] = edge_counts.get(pair_key, 0) + 1
        count = edge_counts[pair_key]

        smooth_opts = False
        if count > 1:
            smooth_opts = {
                "type": "curvedCW",
                "roundness": 0.2 * (count // 2) * (1 if count % 2 == 0 else -1),
            }

        # Get Quantity for Hover
        flow_qty = order_qty_map.get(order, "?")

        # Determine Color/Title
        if mode == "compare":
            color = flow.get("color", "grey")
            width = flow.get("width", 2)
            title = f"{flow['status']}\nOrder: {order}\nQty: {flow_qty}\n{u} -> {v}"
            dashes = True if "magic" in str(u) or "magic" in str(v) else False
        else:
            color = order_color_map.get(order, "black")
            width = 2
            dashes = True if "magic" in str(u) or "magic" in str(v) else False
            if dashes:
                color = "black"
            title = f"Order: {order}\nQty: {flow_qty}\n{u} -> {v}"

        net.add_edge(
            source_id,
            target_id,
            title=title,
            color=color,
            width=width,
            dashes=dashes,
            smooth=smooth_opts,
        )

    # --- 4. Visual Elements ---
    # Time Guides
    for t in range(max_time_step + 1):
        y = t * Y_TIME_SPACING
        net.add_node(
            f"g_{t}_L", x=-100, y=y, label=f"t={t}", shape="text", physics=False
        )
        net.add_node(
            f"g_{t}_R", x=8 * X_SPACING, y=y, label=" ", shape="text", physics=False
        )
        net.add_edge(
            f"g_{t}_L", f"g_{t}_R", color="lightgrey", dashes=True, physics=False
        )

    # Legends
    lx = (max([s["level"] for s in NODE_STYLES.values()]) + 1) * X_SPACING
    ly = 0

    if mode == "compare":
        net.add_node(
            "L_Title",
            label="-- Comparison --",
            x=lx,
            y=ly,
            shape="text",
            font={"size": 20},
            physics=False,
        )
        for lbl, col in [
            ("Match", "green"),
            ("Classical Only", "blue"),
            ("QUBO Only", "red"),
        ]:
            ly += 50
            net.add_node(
                f"L_{lbl}", label=lbl, color=col, x=lx, y=ly, shape="box", physics=False
            )
    else:
        net.add_node(
            "L_Title",
            label="-- Orders --",
            x=lx,
            y=ly,
            shape="text",
            font={"size": 20},
            physics=False,
        )
        for order, color in order_color_map.items():
            ly += 50
            net.add_node(
                f"L_{order}",
                label=f"Order {order}",
                color=color,
                x=lx,
                y=ly,
                shape="box",
                physics=False,
            )

    # Demand Info (Printed on Canvas)
    if demand_records is not None and not demand_records.empty:
        ly += 80
        net.add_node(
            "D_Title",
            label="-- Demand --",
            x=lx,
            y=ly,
            shape="text",
            font={"size": 20},
            physics=False,
        )
        ly += 40

        for i, row in demand_records.iterrows():
            # Robust Extraction
            o_val = row.get("o", row.iloc[0] if len(row) > 0 else "?")
            p_val = row.get("p", row.iloc[1] if len(row) > 1 else None)
            t_val = row.get("t", row.iloc[2] if len(row) > 2 else "?")
            v_val = row.get("v", row.iloc[3] if len(row) > 3 else "?")
            qty = row.get("value", row.iloc[4] if len(row) > 4 else "?")

            if p_val:
                label_text = f"Order {o_val} ({p_val}): {qty}\nat {v_val} by {t_val}"
            else:
                label_text = f"Order {o_val}: {qty}\nat {v_val} by {t_val}"

            net.add_node(
                f"Dem_{i}",
                label=label_text,
                x=lx,
                y=ly,
                shape="text",
                font={"size": 14, "align": "left"},
                physics=False,
            )
            ly += 50

    net.set_options('{"physics": {"enabled": false}}')
    print(f"Generating plot: {filename}")
    net.show(filename)