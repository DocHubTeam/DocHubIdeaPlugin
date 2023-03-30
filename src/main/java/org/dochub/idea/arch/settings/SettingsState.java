package org.dochub.idea.arch.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.refactoring.extractSuperclass.BindToOldUsageInfo;
import com.intellij.util.messages.Topic;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.dochub.idea.arch.markline.LineMarkerNavigator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
        name = "org.dochub.idea.settings.DocHubSettings",
        storages = @Storage("DocHubSettings.xml")
)
public class SettingsState implements PersistentStateComponent<SettingsState> {
    public interface DocHubSettingsMessage {
        void on();
    }
    public static Topic<DocHubSettingsMessage> ON_SETTING_CHANGED = Topic.create("On changed DocHub settings", DocHubSettingsMessage.class);
    public static String modes[] = { "Personal", "Enterprise" };
    public static String defaultUsingMode = "Personal";
    public static String defaultRenderServer = "http://localhost:8079/svg/";
    public static Boolean defaultIsExternalRender = false;
    public static String defaultRenderMode = "Smetana";
    public static Boolean defaultIsEnterprisePortal = false;
    public static String defaultEnterprisePortal = "http://dochub.info";
    public static String renderModes[] = { "Smetana", "ELK", "GraphViz" };
    public String usingMode = SettingsState.defaultUsingMode;
    public String renderMode = SettingsState.defaultRenderMode;
    public Boolean renderIsExternal = SettingsState.defaultIsExternalRender;
    public String serverRendering = SettingsState.defaultRenderServer;
    public String enterprisePortal = SettingsState.defaultEnterprisePortal;

    public static SettingsState getInstance() {
        return ApplicationManager.getApplication().getService(SettingsState.class);
    }

    @Override
    public @Nullable SettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull SettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public Boolean isEnterprise() {
        return this.usingMode.equals("Enterprise");
    }
}
