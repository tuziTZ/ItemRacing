# ItemRacing

灵感和玩法**直接来源**于 [LQSnow](https://github.com/LQSnow/) 的 [BlockRacing](https://github.com/LQSnow/BlockRacing), 向他的创意表示感谢.

___

这是一个 Minecraft 小游戏插件, 玩家分组竞速收集物品, 先完成指定物品数的队伍获胜.

Minecraft 版本: Java `1.21.X`. 服务端: `paper` 及其衍生版本. 游戏人数: `1+`.

> [!WARNING]
> 作者不会 Java , 该项目 90% 以上代码由 Gemini 2.5 Pro 生成, 且插件的所有功能未经充分测试, 插件能运行的环境也未经测试, 作者仅在 `1.21.4` 的 `paper` 服务端上测试了游戏基本功能. 不保证插件的稳定性.

## Screenshots

![screenshot1](screenshots/1.png)

![screenshot2](screenshots/2.png)

![screenshot3](screenshots/3.png)

## 游玩流程

在**准备阶段**和**游戏阶段**, 玩家都可使用 `潜行 + 副手` 打开**游戏菜单**.

1. 准备阶段, 玩家在游戏菜单自由选择队伍, 修改局内游戏设置, 所有人都准备好后即可开始游戏. (1 个人也可以玩.)
2. 游戏阶段, 玩家需收集指定物品, 目标物品会展示于计分板. 玩家可通过游戏菜单或命令提供的功能辅助收集.
3. 允许在游戏开始后, 未加入队伍的玩家加入本局已有的参赛队伍.
4. 管理员可手动中止异常对局；结束后可返回大厅，或重启服务器并删除地图文件夹以重置地图.

## 命令列表

在不同游戏阶段, 玩家均可用 `/help` 命令查看当前可用命令列表.

### 准备阶段

| 命令 | 功能 |
| :---: | :---: |
| /join <队伍名> | 加入队伍 |
| /leave | 离开队伍 |
| /start | 开始游戏 |

### 游戏阶段

|                命令                | 功能 |
|:--------------------------------:| :---: |
|             /locate              | 消耗队伍定位次数, 定位群系或结构. 使用次数可在游戏菜单中购买 |
|               /tpr               | 消耗队伍积分, 在**主世界**随机传送 |
|          /chest \<num>           | 打开第 `num` 个队伍箱子 |
|           /box \<num>            | 同 /chest |
|               /wp                | 打开路径点菜单 |
|             /tp <队友>             | 传送到队友 |
|            /pc \<消息>             | 队伍聊天 |
|          /teammsg \<消息>          | 同 /pc |
| /sb ranking | 显示参赛队伍排行榜 |
| /sb watch <队伍名> | 监视另一支参赛队伍 |
| /sb rotate [1-60秒] | 轮播其他参赛队伍 |
|              /roll               | 申请重摇任务物品 |
|           /join <队伍名>            | 未加入队伍的玩家可在游戏开始后加入游戏 |
| /endgame confirm | 管理员中止当前游戏（默认仅 OP） |

### 结束阶段

|         命令          | 功能 |
|:-------------------:| :---: |
| /resetgame confirm | 清理本局数据并返回大厅（不会还原地图） |
| /itemracing:restart | 重启服务器 |

## 规则设置

### 基本规则

服务器所有者可通过配置文件 `config.yml` 修改以下基本游戏规则:

```yaml
# 队伍名称及颜色
teams:
  red:
    display-name: "§c红队"
    color: "#FF5555"
  blue:
    display-name: "§9蓝队"
    color: "#5555FF"
  green:
    display-name: "§a绿队"
    color: "#55FF55"
  yellow:
    display-name: "§e黄队"
    color: "#FFFF55"

# 大厅中启用的队伍数量（按上方顺序取前 N 支）
enabled-team-count: 4

# 是否为每位玩家解锁全配方
unlock-all-recipes: true

# 定位花费
locate-cost: 3

# 随机传送花费
tpr-cost: 2

# 定位命令可免费使用次数
initial-locate-uses: 1

# 随机传送命令可免费使用次数
initial-tpr-uses: 3


# 团队箱子数量
team-chest-amount: 40

# 团队路径点数量
team-waypoint-amount: 40

# 任务队列大小
task-queue-size: 5

# 默认目标物品数, 可在游戏设置中更改
items-to-collect: 20

# 收集每个物品的得分
score-per-item: 3

# 队伍可 roll 次数
max-rolls-per-team: 5
```

### 局内规则

玩家可在游戏开始前设置（加粗表示默认值）:

1. 任务物品数量 [默认 **20**]
2. 启用队伍数量 [默认读取 `enabled-team-count`]
3. 开局工具(镐斧铲) [无, 石质, **铁质**]
4. 开局鞘翅 [无, 损坏, **正常**]
5. 开局食物 [无, **金胡萝卜**, 饱和]
6. 速度等级 [0~5, 默认 **2**]
7. 急迫等级 [0~5, 默认 **2**]
8. 抗性提升等级 [0~5, 默认 **2**]
9. 初始工具效率等级 [0~5, 默认 **0**]
10. 初始工具耐久等级 [0~3, 默认 **0**]
11. 初始工具经验修补 [默认 **开启**]
12. 初始工具采集附魔 [无, **精准采集**, 时运 I~III]
13. 任务难度模式 [完全随机, **渐进难度**, 严格递增]
14. 玩家物品封禁 [按难度筛选、分页选择，可恢复服务器默认值]

## 物品列表

任务物品由 `plugins/ItemRacing/items.yml` 定义。默认的“渐进难度”模式会让第 i 个任务的期望难度随游戏进度增加；“严格递增”模式会按难度排序，“完全随机”则保留旧玩法。
本局发放的初始工具、鞘翅和金胡萝卜会自动从任务候选中排除，避免开局直接完成任务。

黑名单分为两类：

- `mandatory-exclusions`：技术性、不可获取或必须匹配 NBT 的物品，玩家不能重新启用。
- `default-player-bans`：可以正常取得但默认过难或过稀有的物品，玩家可以在大厅菜单自由启用或封禁。

### 强制排除的物品

- 所有药水
- 所有药水箭
- 所有附魔书
- 迷之炖菜, 成书, 地图, 烟花火箭, 烟火之星
- 所有刷怪蛋、虫蚀方块
- 命令方块、屏障、传送门、基岩等技术性或生存不可获取方块
- 可疑的沙子、可疑的沙砾、紫水晶母岩、青蛙卵等不能正常取得对应物品的方块

### 默认玩家封禁的高难物品

- 所有纹样陶片
- 所有音乐唱片
- 所有锁链装备
- 所有锻造模板
- 所有生物头颅
- 深层煤炭矿石, 深层绿宝石矿石
- 鹦鹉螺壳, 潮涌核心
- 下界合金块
- 龙蛋, 龙息
- 沉重核心, 重锤, 三叉戟
- 下界之星, 信标, 凋零玫瑰
- 不死图腾
- 所有蛙明灯
- 火把花, 瓶子草, 瓶子草荚果, 嗅探兽蛋
- 地球旗帜图案, 涡流旗帜图案, 旋风旗帜图案, 猪鼻旗帜图案, 苦力怕盾徽旗帜图案

物品难度、难度曲线和两类黑名单均可直接修改 `items.yml`，支持 `*`、`?` 通配规则；不再需要修改 `ItemManager.java`。

## 如何使用

有开服经验的话就不用看这部分了, 否则认真看.

### 准备服务器

这里以 `Windows 系统`、`paper` 为例.

前往 [papermc 官网](https://papermc.io/downloads/all) 下载对应版本的服务器核心, 把下载的 `.jar` 移到一个空文件夹中.

在该文件夹处打开终端 (或自行 `cd` 进该目录), 执行:

```powershell
java -jar *.jar
# *.jar 是你下载的服务器核心文件名
```

在看到这样的输出后:

```
[16:58:46 WARN]: Failed to load eula.txt
[16:58:47 INFO]: You need to agree to the EULA in order to run the server. Go to eula.txt for more info.
```

打开同目录下的 `eula.txt`, 把最后一行的 `eula=false` 改成 `eula=true`.

在该目录下新建一个 `start.bat` 批处理文件, 写入如下内容并保存:

```
rd /s /q world
rd /s /q world_nether
rd /s /q world_the_end
java -Xmx4G -Xms4G -jar 你的jar完整文件名 nogui
```

上面的 `4G` 是分配的内存大小, 按你的实际情况进行调整.

以后就可以通过执行 `start.bat` 启动服务器了.

### 安装插件

从 [Releases页](https://github.com/Coast23/ItemRacing/releases) 下载 `ItemRacing-x.x.jar`, 把它放到服务器的 `plugins` 文件夹中.

首次启动服务器后, 会生成 `plugins/ItemRacing/config.yml`, 可根据自己的需求进行配置.

### 重启服务器

使用该功能需要将 `start.sh` 修改为:

* Windows:

```powershell
@echo off
:start

rd /s /q world
rd /s /q world_nether
rd /s /q world_the_end
java -Xmx4G -Xms4G -jar 你的jar完整文件名 nogui

echo.
echo echo Server will restart in 5 seconds... Press Ctrl+C to cancel.
timeout /t 5
goto start
```

* Linux:

```shell
#!/bin/bash

while true
do
    rm -rf world
    rm -rf world_nether
    rm -rf world_the_end
    java -Xmx4G -Xms4G -jar 你的jar完整文件名 nogui
    
    echo "Server will restart in 5 seconds... Press Ctrl+C to cancel."
    sleep 5
done
```
