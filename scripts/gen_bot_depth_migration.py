# -*- coding: utf-8 -*-
"""Generate migration-v6-bot-depth-assign.sql with 100 UPDATEs."""
import os

OPENIDS = [
    "o3inyj35hqwi31ors32vsr3fx5vw",
    "onl189b2cwtgk8x6sdg7bjx98gea",
    "olvx8yd4n0joe2yqlrxmwqmxmd7y",
    "os0lzy0zzav7zn6x8uguypenabf4",
    "o18krnmbcovpjv9bcocdn65blp7n",
    "o78qztqo1uuzlcurpwedwzt4nhd1",
    "onhrme0utnnhlnkrerekpp0h5fvj",
    "oyf1vk5m1dlddpucoup9qyyqjzgb",
    "oprmj7ef2c9winaegpj6bjr8wu6b",
    "ok7a2majv0v7mbwk5s35gewozsc8",
    "onfy2r9dbtt5wc6jqpgqqsqwsm6d",
    "ot9erwkxs7ul1n93ld0t85o3qqwa",
    "ojn8uyxnes8hfcmklmjnlz39an8a",
    "otpwtxmcbhra0dq6x9d2tir40mtl",
    "ofwpiiuycr3fxii7wzct9cs073fw",
    "owp0qn2n127x457x8asdq170o1u0",
    "onifk5xquixmfhlqna4barepa01b",
    "orpwsrpyhk7s6c7g26m4f2upyry1",
    "oe85vcgj1oh38ujv6i956ie4hve1",
    "ou52o82tcqus61897nqjebb7ma2x",
    "oh9drphc7wchymxcp492nr4gquz1",
    "ou6fff1hp0q90sexg1xx2s319zqe",
    "oljsuti8bvzz5rvehv0kpt5f7t7z",
    "o9obgoek4ri5ko8xq62ns2ignxmd",
    "ox2lixl397c2awfm5002c4l2vifn",
    "oddwm90thbp1etrgm2vxj9hjxl5i",
    "owsbo5ktwzk7tn635w7vs8ni9jqr",
    "o9aq4c31klfayr49qnn05byzpsar",
    "ozdmn0rfrrh16q4jg20i165xx5rk",
    "orn0207t9h118etrrfjvhv88szou",
    "oggmx0rn0b713yquxgq4wgjzdjin",
    "oait1vv4hqbc7pqhrn6jb6yf3e5b",
    "oawv7kyo4nmym1golxrb5jybmpja",
    "o8mw15ojktyoh1p6hrl5nnmlw9t6",
    "okioi0q8762f3s917za5wa0di3q7",
    "orkw3sf9pb6uj5om4c3g9cnue65i",
    "or6lmotzjj7f1pnindooapitll21",
    "oupqwdawsvtg8cogi5251bnyxagb",
    "owta6th2yiont6xaoi91mzfu7vak",
    "o41sr3kdkayv9h9hd7hefgx7aco6",
    "ow6dobqg3vtyegoiuzev5y4g1fem",
    "oa51ib75g93ebj096g8ei1zm9zyi",
    "oovyk74xqlipx9dy4dfkewc54ega",
    "ot22j1cnris2yctaxb4iwjeeyv8d",
    "ocji34b5caa7fnpur200c7vodqjj",
    "o6pc0ro922pp18vgyxxk436kv9to",
    "ottuixe801pr74sl5mglbw7uyc80",
    "o9eodtufyfui77vle2zw3vlywypf",
    "o7d2a96v1n0mhaj01fylmzdona67",
    "ot0l9raukfa2shdzj1talychrzoo",
    "omak3s7zmm44o39p4fcuni6xao07",
    "o48ddxakq6ylest0ir9sw0lpjei1",
    "od9r3fvhttnfe37st20l4ux1vdtz",
    "ofyf1ldnlbmrfq0vaxo4zowiio8h",
    "oda9q8v02ddh881ex3y3gm742vpd",
    "oeqb9a3w9kts5ofr3v2iy215a1xr",
    "o6zh1yrc9ff3dozv5wx5ddzki6zv",
    "on8zsh9v59caq1tc1l32z7c2qpb1",
    "ol6bap89bu35pmrxgb80421951h2",
    "o2sn8zyketkj5nvgafiawuuwwvhg",
    "otpvc4kz0aohgpkdwxspry0hhffk",
    "ok8yzg151cd9dutqd1rgzteevgsw",
    "oqh0kg5z56ocqh3cd8b6gnbt8g8y",
    "on516xy29k2edexmtl98d1ybi9b6",
    "oy1jmmwsn21vlqdrtoi8sclwwmj7",
    "onhtj2kbmj0xatm8i4l7mwsfa8aw",
    "o1x5vbm4jj7mtue48bt4x6sfjggg",
    "obe3z7bbm4fl9bu5fu17dcl0zu95",
    "o3elwwvbthjh2wwnh1fnhgscf5dq",
    "o8162l4zi1t33mvfayq74igew7yb",
    "okymdqrtrm6s014u7n0c8ym24byv",
    "ofucfalmrxblg700b0kymb83o63t",
    "oijlyhenkzuumvht7bqm7f38qvy4",
    "oh2vur88itth5xfp4sacw320cj94",
    "o2vj6gd0w8orlcw1weiky8zw6kff",
    "ogtya6ob2uknviulgg3fg9f2typ2",
    "oa9b5ijzsh0zz2g4vqd6opyk5agg",
    "oa2v4cg78ln3cq5srz7eaaosfr8f",
    "oo4wbhqkgdv4mt1s1artswqdb777",
    "oj5bwp7qkuaiat2drf3w78ap1imi",
    "opwmf2a3wz4ma10bh4q7vf1xaijz",
    "ojvl36w71uo489vmelyuo9j2liog",
    "odyitz0ul8at0o63fc5p3siais01",
    "oq8unmrrj6gpynu5rkg0zb1l2a4h",
    "ozjiimg39o8w4h4dc1125yzrir1x",
    "oy2fb9wezdg8ez51b65g9x21bpdz",
    "ovbx3v1tq4a5pkcippsjo0fp7kn5",
    "o2ceau9uyet67zja4dy4qp2f8cj4",
    "oirftgl4ruzc7v4vhw36kmmc80jy",
    "ojxmmbizmyxuwx3y1956jv7hv4yb",
    "odjj87881qqlvwqjzb0iwxoe95b7",
    "ozmcl3jmy1vojfpzprhv67cpzsmp",
    "o4p37yauw78d7ymjmnly76giy2r9",
    "o4yt5i02iwsjgzawysmci1fmgjzy",
    "oncy3zxb32lke161szbgm8v420yn",
    "ow70awqxawdllewp7223lgw22t9d",
    "o7c37hytmpy72ctbn3ombec0mf2l",
    "on2lwvn6vhmvf4vurgyq3xdvzuxr",
    "omgwshi1kvoghmgvd50vxv3ra14z",
    "ocv8pyr2v29rpazqm7ex1rh8qvgg",
]

def main():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    out = os.path.join(root, "src", "main", "resources", "sql", "migration-v6-bot-depth-assign.sql")
    lines = [
        "-- 100 人机账号 bot_search_depth 区间（须已执行 migration-v5-bot-search-depth.sql）",
        "-- 与 GomokuAiEngine.ABS_MAX_BOT_SEARCH_DEPTH=12 一致；每人固定区间，对局内每步在 [min,max] 随机",
        "",
    ]
    assert len(OPENIDS) == 100
    for i in range(100):
        m = 1 + i % 7
        s = (i // 7) % (13 - m)
        hi = m + s
        oid = OPENIDS[i]
        lines.append(
            "UPDATE `users` SET `bot_search_depth_min` = %d, `bot_search_depth_max` = %d "
            "WHERE `openid` = '%s' AND `is_bot` = 1;"
            % (m, hi, oid)
        )
    with open(out, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines) + "\n")
    print("Wrote", out)


if __name__ == "__main__":
    main()
