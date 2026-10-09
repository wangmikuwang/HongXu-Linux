package io.wenyou.textquest

import io.wenyou.textquest.data.model.*
import io.wenyou.textquest.ui.common.*
import org.junit.Assert.*
import org.junit.Test

class StoryBranchTreeTest {
    @Test fun cyclesMergesMissingAndConditionalExitsAreShownWithoutDuplicatingSubtrees() {
        val nodes = linkedMapOf(
            "start" to StoryNode(id = "start", choices = listOf(ChoiceData("左", "a"), ChoiceData("右", "b"), ChoiceData("留在原地", "@self"), ChoiceData("空目标"))),
            "a" to StoryNode(id = "a", choices = listOf(ChoiceData("到结局", "end"))),
            "b" to StoryNode(id = "b", choices = listOf(ChoiceData("合流", "end"), ChoiceData("损坏目标", "missing"))),
            "end" to StoryNode(id = "end", kind = NodeKind.ENDING, endTarget = "start"),
            "orphan" to StoryNode(id = "orphan", endTarget = "ai"),
            "ai" to StoryNode(id = "ai", kind = NodeKind.AI, endTarget = "end"))
        val story = Story(id = "test", title = "test", nodes = nodes)
        val rows = storyBranches(story)
        assertEquals(nodes.keys, rows.filter { it.kind == BranchKind.NODE }.map { it.nodeId }.toSet())
        assertEquals(2, rows.count { it.kind == BranchKind.CYCLE })
        assertEquals(2, rows.count { it.kind == BranchKind.MERGE })
        assertEquals("missing", rows.single { it.kind == BranchKind.MISSING }.nodeId)
        assertEquals(1, rows.count { it.kind == BranchKind.DYNAMIC })
        assertEquals("未从起点连接", rows.single { it.nodeId == "orphan" }.label)
        assertFalse(rows.any { it.label == "自动跳转" && it.nodeId == "start" })
        val collapsed = visibleBranches(rows, setOf("start"))
        assertEquals(listOf("start", "orphan"), collapsed.filter { it.depth == 0 }.map { it.nodeId })
        assertFalse(collapsed.any { it.nodeId == "a" })
        val gated = StoryNode(id = "start", choices = listOf(ChoiceData("条件", "end", conditions = listOf(Cond(name = "flag")))), endTarget = "b")
        assertTrue(storyBranches(story.copy(nodes = nodes + ("start" to gated))).any { it.label == "无可用选项时自动跳转" && it.nodeId == "b" })
        assertFalse(storyBranches(story.copy(nodes = nodes + ("start" to gated.copy(choices = gated.choices + ChoiceData("始终可用", "end"))))).any { it.label == "无可用选项时自动跳转" })
        val director = storyBranches(story.copy(mode = StoryMode.AI_DIRECTOR))
        assertTrue(director.filter { it.kind == BranchKind.NODE }.all { it.depth == 0 })
        assertEquals(BranchKind.MISSING, storyBranches(story.copy(startNodeId = "absent")).first().kind)
        val deep = (0..5000).associate { i -> "n$i" to StoryNode(id = "n$i", endTarget = if (i < 5000) "n${i+1}" else "") }
        assertEquals(5001, storyBranches(story.copy(startNodeId = "n0", nodes = deep)).size)
    }
}
