package org.dochub.idea.arch.markline

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.resolve.FileContextUtil.INJECTED_IN_ELEMENT
import org.dochub.idea.arch.jsonata.JSONataFile
import org.dochub.idea.arch.utils.YAMLUtil
import org.jetbrains.yaml.psi.YAMLKeyValue
import kotlin.io.encoding.Base64.Default.UrlSafe
import kotlin.io.encoding.ExperimentalEncodingApi

class JSONataLineMarkerProvider: LineMarkerProvider {
    @OptIn(ExperimentalEncodingApi::class)
    override fun getLineMarkerInfo(element: PsiElement) =
        (element as? JSONataFile)?.run {
            LineMarkerInfo(
                element,
                TextRange(0, 1),
                AllIcons.Actions.Execute,
                { "Выполнить" },
                { _ , elt ->
                    //val yamlElement = elt.get()[INJECTED_IN_ELEMENT]?.element?.parent
                    val yamlElement = elt.containingFile.getUserData(INJECTED_IN_ELEMENT)?.element?.parent
                    if ((yamlElement as? YAMLKeyValue)?.keyText == "source") {
                        (yamlElement.parent?.parent as? YAMLKeyValue)?.run {
                            elt.project
                                .messageBus
                                .syncPublisher(LineMarkerNavigator.ON_NAVIGATE_MESSAGE)
                                .go("devtool", "source:${YAMLUtil.getConfigFullName(this)}")
                        }
                    } else {
                        elt.project
                            .messageBus
                            .syncPublisher(LineMarkerNavigator.ON_NAVIGATE_MESSAGE)
                            .go("devtool", "file:${UrlSafe.encode(elt.text.toByteArray())}")
                    }
                },
                GutterIconRenderer.Alignment.LEFT
            ) { "DocHub" }
        }

}
