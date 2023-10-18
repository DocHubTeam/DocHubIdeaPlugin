package org.dochub.idea.arch.references;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class References {
    public static class Reference {
        public String entity;
        public ElementPattern<? extends PsiElement> pattern;
    }

    public static List<Reference> getRefs(Project project) {
        CachedValue<ArrayList<Reference>> cache = (CachedValue<ArrayList<Reference>>) project.getUserData(Consts.cacheKey);
        return cache != null ? cache.getValue(): new ArrayList<Reference>();
    }
    public static void updateRefs(Project project, String schema) {
        CachedValue<ArrayList<Reference>> cache = CachedValuesManager.getManager(project).createCachedValue(() -> {
            ArrayList<Reference> items = new ArrayList<>();
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonSchema = mapper.readTree(schema);
                JsonNode jsonRels = jsonSchema.get("$rels");
                if (jsonRels != null) {
                    Iterator<String> iterator = jsonRels.fieldNames();
                    iterator.forEachRemaining(e -> {
                        String[] domains = e.split("\\.");
                        Reference ref =new Reference();
                        ref.entity = String.join(".", Arrays.copyOfRange(domains, 0, domains.length - 1));
                        ref.pattern = PlatformPatterns.psiElement(YAMLKeyValue.class)
                                .withParent(PlatformPatterns.psiElement(YAMLMapping.class))
                                .withSuperParent(2,
                                        PlatformPatterns.psiElement(YAMLKeyValue.class)
                                                .withName(PlatformPatterns.string().equalTo(ref.entity))
                                                .withSuperParent(2, PlatformPatterns.psiElement(YAMLDocument.class))
                                );
                        items.add(ref);
                    });
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            return CachedValueProvider.Result.create(
                    items,
                    PsiModificationTracker.NEVER_CHANGED);
        }, false);
        project.putUserData(Consts.cacheKey, cache);
    }
}
