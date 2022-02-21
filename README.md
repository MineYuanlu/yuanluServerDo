# yuanluerServerDo

让跨服玩家无缝执行命令

统计数据

- [bstats Bukkit](https://bstats.org/plugin/bukkit/yuanluServerDo/12395)
- [bstats Bungee](https://bstats.org/plugin/bungeecord/yuanluServerDo/12396)
- ![最新版本 badge](https://update.yuanlu.bid/ico/v/mc-bukkit/yuanluServerDo "最新版本")
- ![bStats Players](https://img.shields.io/bstats/players/12396?label=%E7%8E%A9%E5%AE%B6%E6%95%B0%E9%87%8F) ![bStats Servers](https://img.shields.io/bstats/servers/12396?label=%E6%9C%8D%E5%8A%A1%E5%99%A8%E6%95%B0%E9%87%8F)

## 替代yuanluServerTp

本插件继承了yuanluServerTp思想, 重构后可完全替代此插件

- /tp \<target\>
- /tp \<mover\> \<target\>
- /tpa \<target\>
- /tphere \<target\>
- /tpahere \<target\>
- /tpaccept \[who\]
- /tpdeny \[who\]
- /tpcancel \[who\]

## 实现了列表隐身

使用/ysd-v \[always\] 命令可以隐藏自己的在线状态, 防止被传送请求发现

## 实现跨服Home及Warp

本插件实现了Home及Warp的相关功能, 可以跨服传送至家或地标

- /home
- /home \<home\>
- /sethome \[home\]
- /delhome \<home\>
- /warp
- /warp \<warp\>
- /setwarp \<warp\>
- /delhome \<warp\>

## 实现第三方数据转换

本插件实现了从第三方插件转换数据

命令: /ysd-trans \<plugin\> \<func\>

当前支持的插件及数据:

- CMI
	- Home
	- Warp

## 实现了At功能

在聊天中使用@+玩家名, 可以向对方发送提示音