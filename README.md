# yuanluerServerDo

让跨服玩家无缝执行命令

统计数据

- [bstats Bukkit](https://bstats.org/plugin/bukkit/yuanluServerDo/12395)
- [bstats Bungee](https://bstats.org/plugin/bungeecord/yuanluServerDo/12396)

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
