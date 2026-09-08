"""市场技术指标计算，不依赖行情供应商。"""

def calc_ema(data, period):
    """计算指数移动平均（EMA）

    公式：
        k = 2 / (period + 1)
        EMA[0..period-2] = data[0..period-2]（初始填充）
        EMA[period-1] = SMA(data[:period])（首个有效值为前period期的简单平均）
        EMA[i] = (data[i] - EMA[i-1]) * k + EMA[i-1]  （i >= period）

    Args:
        data: 输入价格序列
        period: EMA周期

    Returns:
        EMA序列（长度与data一致）
    """
    k = 2.0 / (period + 1)
    ema = [0.0] * len(data)
    ema[period - 1] = sum(data[:period]) / period
    for i in range(period, len(data)):
        ema[i] = (data[i] - ema[i - 1]) * k + ema[i - 1]
    for i in range(period - 1):
        ema[i] = data[i]
    return ema


def calc_macd(closes):
    """计算MACD指标并生成信号描述

    MACD由三部分组成：
        - DIF（快线）= EMA12 - EMA26
        - DEA（慢线/信号线）= EMA9(DIF)
        - MACD柱（BAR）= 2 * (DIF - DEA)

    信号规则：
        - DIF零轴位置判断中长期趋势方向
        - DIF与DEA交叉判断金叉/死叉
        - 柱状图颜色和变化判断多空动能

    Args:
        closes: 收盘价序列（至少26个数据点）

    Returns:
        dict: 包含 dif / dea / bar / signal 四个字段
              数据不足时返回零值和提示信息
    """
    if len(closes) < 26:
        return {"dif": 0, "dea": 0, "bar": 0, "signal": "数据不足26周期，无法计算MACD"}

    ema12 = calc_ema(closes, 12)
    ema26 = calc_ema(closes, 26)
    dif = [ema12[i] - ema26[i] for i in range(len(closes))]
    dea = calc_ema(dif, 9)
    bar = [2 * (dif[i] - dea[i]) for i in range(len(closes))]

    cur = len(closes) - 1
    prv = cur - 1

    # 信号生成
    parts = []
    if dif[cur] > 0:
        parts.append("DIF在零轴上方")
    elif dif[cur] < 0:
        parts.append("DIF在零轴下方")
    else:
        parts.append("DIF处于零轴")

    if prv >= 0:
        if dif[prv] < dea[prv] and dif[cur] >= dea[cur]:
            parts.append("DIF上穿DEA形成金叉")
        elif dif[prv] > dea[prv] and dif[cur] <= dea[cur]:
            parts.append("DIF下穿DEA形成死叉")
        elif dif[cur] > dea[cur]:
            parts.append("DIF在DEA上方呈多头排列")
        elif dif[cur] < dea[cur]:
            parts.append("DIF在DEA下方呈空头排列")
        else:
            parts.append("DIF与DEA粘合")
    else:
        parts.append("DIF与DEA粘合")

    if bar[cur] > 0:
        parts.append("柱状图为正值（红柱）")
        if bar[cur] > bar[prv]:
            parts.append("且放大，多头动能增强")
        else:
            parts.append("但缩小，多头动能减弱")
    elif bar[cur] < 0:
        parts.append("柱状图为负值（绿柱）")
        if bar[cur] < bar[prv]:
            parts.append("且放大，空头动能增强")
        else:
            parts.append("但缩小，空头动能减弱")
    else:
        parts.append("柱状图归零，多空平衡")

    return {
        "dif": round(dif[cur], 4),
        "dea": round(dea[cur], 4),
        "bar": round(bar[cur], 4),
        "signal": "，".join(parts),
    }
