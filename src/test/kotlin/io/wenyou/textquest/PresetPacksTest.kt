package io.wenyou.textquest

import io.wenyou.textquest.data.model.AppBundle
import io.wenyou.textquest.data.model.AppJson
import io.wenyou.textquest.data.model.NodeKind
import io.wenyou.textquest.data.model.StoryMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Every bundled preset pack parses, references only its own characters, and every branching script can reach an ending. */
class PresetPacksTest {
    private val src = listOf(File("src"), File("app/src")).first(File::isDirectory)
    private val packs = src.walkTopDown().filter { it.invariantSeparatorsPath.contains("/resources/presets/") && it.extension == "json" }.toList()

    @Test fun packsAreSelfContainedAndPlayable() {
        assertTrue(packs.size >= 3)
        val storyIds = mutableSetOf<String>()
        for (file in packs) {
            val pack = AppJson.decodeFromString(AppBundle.serializer(), file.readText())
            val ids = pack.characters.map { it.id }.toSet()
            for (story in pack.stories) {
                assertTrue("${file.name}: duplicate story ${story.id}", storyIds.add(story.id))
                assertTrue("${file.name}: ${story.id} references a missing character", ids.containsAll(story.characterIds))
                assertTrue("${file.name}: ${story.id} has no start node", story.startNodeId in story.nodes)
                story.nodes.values.mapNotNull { it.speakerId.takeIf(String::isNotBlank) }.forEach {
                    assertTrue("${file.name}: ${story.id} speaker $it is not in the pack", it in ids)
                }
                if (story.mode != StoryMode.SCRIPT) continue
                val reached = mutableSetOf<String>()
                val todo = ArrayDeque(listOf(story.startNodeId))
                while (todo.isNotEmpty()) {
                    val id = todo.removeFirst()
                    if (!reached.add(id)) continue
                    val node = story.nodes.getValue(id)
                    if (node.kind != NodeKind.ENDING) assertTrue("${file.name}: ${story.id}/$id is a dead end", node.choices.isNotEmpty())
                    node.choices.forEach { assertTrue("${file.name}: ${story.id}/$id points to missing ${it.next}", it.next in story.nodes); todo += it.next }
                }
                assertEquals("${file.name}: ${story.id} has unreachable nodes", story.nodes.keys, reached)
                assertTrue("${file.name}: ${story.id} cannot end", reached.any { story.nodes.getValue(it).kind == NodeKind.ENDING })
            }
        }
    }
}
