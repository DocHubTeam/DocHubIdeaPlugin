package org.dochub.idea.arch.markline;

import com.intellij.codeInsight.daemon.*;
import com.intellij.psi.PsiElement;
import org.dochub.idea.arch.references.References;
import org.dochub.idea.arch.references.providers.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.dochub.idea.arch.markline.LineMarkerNavigator.*;

public class LineMarkerYaml extends LineMarkerProviderDescriptor {

    @Override
    public final void collectSlowLineMarkers(@NotNull List<? extends PsiElement> elements, @NotNull Collection<? super LineMarkerInfo<?>> result) {
        for (int i = 0, size = elements.size(); i < size; i++) {
            PsiElement element = elements.get(i);
            collectNavigationMarkers(element, result);
        }
    }

    protected void collectNavigationMarkers(
            @NotNull PsiElement element,
            @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result
    ) {
    }

    @Override
    public String getName() {
        return null;
    }

    private boolean isRegisteredComponent(@NotNull PsiElement element, String id) {
//        Map<String, Object> cache = CacheBuilder.getProjectCache(element.getProject());
//        Map<String, Object> components = cache == null ? null : (Map<String, Object>) cache.get("components");
//        PsiElement document = PsiUtils.getYamlDocumentByPsiElement(element);
//        List<String> suggest = SuggestUtils.scanYamlPsiTreeToID(document, "components");
//        return components.get(id) != null || (suggest.indexOf(id) >= 0);
        return true; // todo Здесь нужно проверять на действительную регистрацию компонента
    }

    private boolean isRegisteredDocument(@NotNull PsiElement element, String id) {
        return true; // todo Здесь нужно проверять на действительную регистрацию компонента
    }

    public interface ElementExplain {
        default LineMarkerNavigator.DocHubNavigationHandler register(String id) {
            return null;
        };
    }

    public LineMarkerInfo explainElement(@NotNull PsiElement element, ElementExplain explain) {
        LineMarkerInfo result = null;
        String id = null;
        PsiElement markElement = element;
        if (element instanceof YAMLKeyValue) {
            markElement = element.getFirstChild();
            id = ((YAMLKeyValue)element).getName();
        } else if (element instanceof YAMLPlainTextImpl) {
            markElement = element.getFirstChild();
            id = element.getText();
        }
        if (id != null && isRegisteredComponent(element, id)) {
            result = makeLineMarkerInfo(
                    explain.register(id),
                    markElement
            );
        }
        return result;
    }

    private LineMarkerInfo getLineMarkerInfoForComponent(@NotNull PsiElement element) {
        return explainElement(element, new ElementExplain() {
            @Override
            public DocHubNavigationHandler register(String id) {
                return new DocHubNavigationHandler("component", id);
            }
        });
    }

    private LineMarkerInfo getLineMarkerInfoForDocument(@NotNull PsiElement element) {
        return explainElement(element, new ElementExplain() {
            @Override
            public DocHubNavigationHandler register(String id) {
                return new DocHubNavigationHandler("document", id);
            }
        });
    }

    private LineMarkerInfo getLineMarkerInfoForAspect(@NotNull PsiElement element) {
        return explainElement(element, new ElementExplain() {
            @Override
            public DocHubNavigationHandler register(String id) {
                return new DocHubNavigationHandler("aspect", id);
            }
        });
    }

    private LineMarkerInfo getLineMarkerInfoForContext(@NotNull PsiElement element) {
        return explainElement(element, new ElementExplain() {
            @Override
            public DocHubNavigationHandler register(String id) {
                return new DocHubNavigationHandler("context", id);
            }
        });
    }

    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        LineMarkerInfo result = null;

        if (RefComponentID.pattern().accepts(element)) {
            result = getLineMarkerInfoForComponent(element);
        } else if (RefDocsID.pattern().accepts(element)) {
            result = getLineMarkerInfoForDocument(element);
        } else if (RefAspectID.pattern().accepts(element)) {
            result = getLineMarkerInfoForAspect(element);
        } else if (RefContextID.pattern().accepts(element)) {
            result = getLineMarkerInfoForContext(element);
        } else {
            List<References.Reference> refs = References.getRefs(element.getProject());
            for (int f = 0; f < refs.size(); f++) {
                References.Reference ref = refs.get(f);
                if (ref.pattern.accepts(element)) {
                    result = explainElement(element, new ElementExplain() {
                        @Override
                        public DocHubNavigationHandler register(String id) {
                            return new DocHubNavigationHandler("entity/" + ref.entity, id);
                        }
                    });
                    break;
                }
            }
        }
        return result;
    }
}
