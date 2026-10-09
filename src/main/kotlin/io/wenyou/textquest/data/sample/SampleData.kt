package io.wenyou.textquest.data.sample

import io.wenyou.textquest.data.model.AiStorySettings
import io.wenyou.textquest.data.model.CharacterData
import io.wenyou.textquest.data.model.ChoiceData
import io.wenyou.textquest.data.model.CompareOp
import io.wenyou.textquest.data.model.Cond
import io.wenyou.textquest.data.model.CondType
import io.wenyou.textquest.data.model.Effect
import io.wenyou.textquest.data.model.EffectType
import io.wenyou.textquest.data.model.NodeKind
import io.wenyou.textquest.data.model.Story
import io.wenyou.textquest.data.model.StoryMode
import io.wenyou.textquest.data.model.StoryNode

/**
 * 首次启动植入的示例内容：一个「分支剧本」（演示变量/条件/掷骰/AI 增强节点
 * 与多个结局）和一个「AI 导演」自由模式剧本，以及配套角色卡。
 */
object SampleData {

    val characters: List<CharacterData> = listOf(
        CharacterData(
            id = "char-lin", name = "林晚秋", emoji = "🍂", colorIndex = 4,
            tagline = "老城咖啡馆里写悬疑小说的神秘常客",
            personality = "温润克制、观察力敏锐；习惯把情绪藏进故事里；对陌生人礼貌疏离，熟了会变得很会照顾人。",
            speechStyle = "语速不快，常用比喻；说到兴致处会微微笑；从不大声，但话里有话。",
            background = "出版过三本悬疑小说的作家，最近总在写一个『雨夜与伞』的故事，似乎与某个没等到的人有关。",
            exampleDialogue = "「雨声会把人的秘密洗出来。你信么？」",
            greeting = "「好巧，又下雨了。」"
        ),
        CharacterData(
            id = "char-boss", name = "阿蛮", emoji = "🍵", colorIndex = 8,
            tagline = "咖啡馆老板娘，嗓门大心肠软",
            personality = "爽利热情、爱操心，把每个熟客都当孩子看。",
            speechStyle = "大嗓门、爱用叹词，动不动就“哎哟喂”。",
            background = "在这条老街开了十二年咖啡馆，认识这条街上每一个人。",
            exampleDialogue = "「哎哟喂，这雨下得跟不要钱似的！」"
        ),
        CharacterData(
            id = "char-candle", name = "烛影", emoji = "🕯️", colorIndex = 0,
            tagline = "漂浮雾城「忆城」的守梦人",
            personality = "神秘而温柔，说话像吟诗；记得所有来访者的旧梦，却总记不起自己的。",
            speechStyle = "慢、轻、带诗意的句子；偶尔沉默很久。",
            background = "忆城唯一的居民。每个在雾夜迷路的人都会遇见他，他负责帮你找回想找的东西——或让你忘掉。",
            exampleDialogue = "「梦太重的人，走不出雾。要不要，先把灯点起来？」"
        )
    )

    // ---------------- 示例剧本：雨夜咖啡馆（分支 + AI 增强） ----------------

    // 全局 intimacy 变量 + 角色「林晚秋」的好感度（角色状态，可在对局抽屉查看）
    private val addIntimacy = listOf(
        Effect(EffectType.ADD_VAR, name = "intimacy", value = 1.0),
        Effect(EffectType.ADD_VAR, name = "affection", value = 1.0, charId = "char-lin")
    )

    fun cafeStory(): Story {
        val nodes = linkedMapOf<String, StoryNode>()
        fun put(n: StoryNode) { nodes[n.id] = n }

        put(
            StoryNode(
                id = "start", kind = NodeKind.NARRATION, title = "雨夜",
                text = "雨敲着玻璃，晚上九点，老城咖啡馆里只剩你和她。\n老板娘阿蛮打了个哈欠，柜台里的收音机放着沙沙的爵士。\n角落里的常客——林晚秋，合上了那沓手稿，抬头朝你微微点了点头。",
                choices = listOf(
                    ChoiceData("走过去坐下：「好巧，又下雨了。」", next = "talk"),
                    ChoiceData("安静喝完这杯，观察她一会儿就离开", next = "observe",
                        effects = listOf(Effect(EffectType.SET_FLAG, name = "quiet"))),
                    ChoiceData("帮老板娘收拾，准备打烊", next = "help")
                )
            )
        )
        put(
            StoryNode(
                id = "talk", kind = NodeKind.NARRATION, title = "落座",
                speakerId = "char-lin", text = "她替你续了半杯热茶：「坐吧，正好陪我听听雨。」\n雨声把整间屋子压得很安静，只剩你们之间这点热气。",
                onEnter = addIntimacy,
                choices = listOf(
                    ChoiceData("聊聊她那沓手稿", next = "novel"),
                    ChoiceData("问她是不是常一个人来", next = "talkmore"),
                    ChoiceData("喝完这杯就走", next = "leave")
                )
            )
        )
        put(
            StoryNode(
                id = "talkmore", kind = NodeKind.NARRATION, title = "她与老街",
                speakerId = "char-lin",
                text = "「嗯……这家店能看到老城最老的梧桐。」她望向窗外，「人少，安静，适合想事。」",
                onEnter = addIntimacy,
                choices = listOf(
                    ChoiceData("追问一句：「你在写故事吧？」", next = "novel"),
                    ChoiceData("（好感足够时）轻声问：“那个人……你还在等吗？”", next = "deep",
                        conditions = listOf(Cond(CondType.VAR, name = "intimacy", op = CompareOp.GTE, value = 3.0))),
                    ChoiceData("起身告辞", next = "leave")
                )
            )
        )
        put(
            StoryNode(
                id = "novel", kind = NodeKind.AI, title = "手稿的秘密",
                speakerId = "char-lin",
                prompt = "林晚秋被看穿了写小说的秘密，有些惊讶又有些放松。请描写她手稿里的那个『雨夜与伞』的悬疑故事，并展开一段自然深入的对话——她试探你，你也慢慢猜到她故事里那个没有结尾的人是谁。",
                endTarget = "deep"
            )
        )
        put(
            StoryNode(
                id = "deep", kind = NodeKind.NARRATION, title = "没有结尾的结局",
                speakerId = "char-lin",
                text = "「那个侦探，总在雨夜遇到同一把伞。」她说，「他大概在等一个答案。」\n她看着你，目光里有一点像雨的东西，\${intimacy} 分信任就这么轻轻落了下来。",
                onEnter = addIntimacy,
                choices = listOf(
                    ChoiceData("「那把伞，会不会是在等一个人来告别？」", next = "ending_kind"),
                    ChoiceData("岔开话题，聊聊别的", next = "leave")
                )
            )
        )
        put(
            StoryNode(
                id = "observe", kind = NodeKind.NARRATION, title = "远远地看",
                text = "你喝完咖啡，余光里她的侧影很安静。窗外雨线倾斜，她把那盏小台灯往自己那边挪了挪。",
                choices = listOf(
                    ChoiceData("离开前，留一张字条和你的伞", next = "ending_note"),
                    ChoiceData("悄悄结账，走进雨里", next = "ending_quiet")
                )
            )
        )
        put(
            StoryNode(
                id = "help", kind = NodeKind.NARRATION, title = "打烊时分",
                text = "你挽起袖子帮阿蛮拖地、收杯。没想到她也站了起来，默默一起收拾。\n雨夜的灯下，你们默契得像是认识了许多年。默契度：\${默契}",
                onEnter = listOf(Effect(EffectType.ROLL, name = "默契", to = 6.0)),
                choices = listOf(
                    ChoiceData("打趣她：「这么贤惠，小说里的大侦探也这样？」", next = "ending_team",
                        conditions = listOf(Cond(CondType.VAR, name = "默契", op = CompareOp.GTE, value = 4.0))),
                    ChoiceData("谢过她，各自回家", next = "ending_farewell")
                )
            )
        )
        put(
            StoryNode(
                id = "leave", kind = NodeKind.NARRATION, title = "门檐下",
                text = "雨小了些。她站在咖啡馆门口，把手里的伞往你这边让了让。",
                choices = listOf(
                    ChoiceData("撑起同一把伞，送她回家", next = "ending_kind"),
                    ChoiceData("（好感足够时）约她改天再喝一杯", next = "ending_note",
                        conditions = listOf(Cond(CondType.VAR, name = "intimacy", op = CompareOp.GTE, value = 2.0))),
                    ChoiceData("就此道别", next = "ending_farewell")
                )
            )
        )

        put(StoryNode(id = "ending_kind", kind = NodeKind.ENDING, title = "结局 · 同一把伞",
            text = "伞下只有你们两个人的脚步声。她忽然说：「故事写完了——结尾是，他终于等到了那个答案。」\n你听见雨声里，有什么东西轻轻落了地。"))
        put(StoryNode(id = "ending_note", kind = NodeKind.ENDING, title = "结局 · 未寄出的信",
            text = "第二天晴了。你收到一张字条，上面只有一行字：「那把伞，我留下了。下次下雨，还来。」"))
        put(StoryNode(id = "ending_quiet", kind = NodeKind.ENDING, title = "结局 · 各自安好",
            text = "你走进雨里，没有回头。有些相遇，本就该留在那个雨夜。"))
        put(StoryNode(id = "ending_team", kind = NodeKind.ENDING, title = "结局 · 默契的两个人",
            text = "阿蛮在柜台后笑出一脸褶子：「哎哟喂，你俩这默契，改天把婚结了，店都给你们！」\n雨停了，城市的灯一盏盏亮起来。"))
        put(StoryNode(id = "ending_farewell", kind = NodeKind.ENDING, title = "结局 · 雨停之后",
            text = "「晚安。」她说。\n你点点头，走进湿润的夜色里。身后咖啡馆的灯，又亮了一会儿才熄。"))

        return Story(
            id = "story-cafe",
            title = "雨夜咖啡馆",
            subtitle = "在雨停之前，把话说完",
            coverEmoji = "☕",
            colorIndex = 4,
            genre = "都市 · 治愈 · 悬疑",
            mode = StoryMode.SCRIPT,
            characterIds = listOf("char-lin", "char-boss"),
            startNodeId = "start",
            nodes = nodes,
            initialVariables = mapOf("intimacy" to 0.0),
            ai = AiStorySettings(
                worldSummary = "老城的一家小咖啡馆。连日下雨，晚上九点后店里常只剩一个写悬疑小说的姑娘林晚秋。玩家的选择会影响你们的亲近程度（变量 intimacy）以及故事走向，共有 5 种结局。",
                tone = "细腻克制的都市叙事，中文，短段落，重视雨声与灯光的氛围。",
                maxTokens = 700
            )
        )
    }

    // ---------------- 示例剧本：忆城（AI 导演自由模式） ----------------

    fun dreamStory(): Story = Story(
        id = "story-dream",
        title = "雾城 · 忆城",
        subtitle = "AI 导演模式：自由输入，剧情由你与导演共同书写",
        coverEmoji = "🌫️",
        colorIndex = 0,
        genre = "奇幻 · 冒险 · 开放式",
        mode = StoryMode.AI_DIRECTOR,
        characterIds = listOf("char-candle"),
        startNodeId = "start",
        nodes = mapOf(
            "start" to StoryNode(
                id = "start", kind = NodeKind.NARRATION, title = "醒来",
                text = "你在雾中醒来。脚下是一座漂在云海上的旧城，街灯都点着暖黄色的火。\n一个披着灰袍的人提着灯，安静地站在你面前——烛影。\n「你迷路了。」他说，「不过在忆城，迷路的人最后总能找到一样东西：要么是他想找的，要么是他该忘的。」\n他开始向你描述这座城……你想做什么、问什么，都可以直接告诉他。"
            )
        ),
        ai = AiStorySettings(
            worldSummary = "忆城：一座漂浮在雾海上的梦之城，居民都是迷路的访客与不愿醒来的记忆。守梦人烛影引路。玩家可以探索、寻找自己丢失的记忆、帮助其他梦中人，也可以随时让故事走向任何结局。",
            tone = "诗意的中文奇幻叙事，第二人称『你』，注意悬念与画面感，每轮 2-4 段为宜。",
            directorExtra = "玩家可能做出任何事，请尊重并自然接住；重要抉择出现时给出 2-4 个选项作为灵感；除非玩家明确结束，否则不要强行收尾。",
            maxTokens = 1100
        )
    )

    fun all(): Pair<List<CharacterData>, List<Story>> = characters to listOf(cafeStory(), dreamStory())
}
