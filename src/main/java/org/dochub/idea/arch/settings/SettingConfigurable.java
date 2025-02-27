package org.dochub.idea.arch.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SettingConfigurable implements Configurable {

    private SettingComponent settingComponent;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "DocHub: Plugin settings";
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return settingComponent.getPreferredFocusedComponent();
    }

    @Override
    public @Nullable JComponent createComponent() {
        settingComponent = new SettingComponent();
        return settingComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        SettingsState settingsState = SettingsState.getInstance();
        boolean modified = !settingComponent.getRenderServerText().equals(settingsState.serverRendering);
        modified |= !settingComponent.getRenderServerRequestTypeText().equals(settingsState.renderServerRequestType);
        modified |= settingComponent.getIsExternalRenderBool() != settingsState.renderIsExternal;
        modified |= settingComponent.getEnterprisePortalText() != settingsState.enterprisePortal;
        modified |= !settingComponent.getUsingModeText().equals(settingsState.usingMode);
        modified |= !settingComponent.getGitServerModeText().equals(settingsState.gitServerMode);
        modified |= !settingComponent.getGitServerText().equals(settingsState.gitServer);
        modified |= !settingComponent.getGitPersonalToken().equals(settingsState.gitPersonalToken);
        return modified;
    }

    @Override
    public void apply() throws ConfigurationException {
        SettingsState settingsState = SettingsState.getInstance();
        settingsState.renderServerRequestType = settingComponent.getRenderServerRequestTypeText();
        settingsState.serverRendering = settingComponent.getRenderServerText();
        settingsState.renderIsExternal = settingComponent.getIsExternalRenderBool();
        settingsState.usingMode = settingComponent.getUsingModeText();
        settingsState.enterprisePortal = settingComponent.getEnterprisePortalText();
        settingsState.gitServerMode = settingComponent.getGitServerModeText();
        settingsState.gitServer = settingComponent.getGitServerText();
        settingsState.gitPersonalToken = settingComponent.getGitPersonalToken();
        SettingsState.DocHubSettingsMessage publisher = ApplicationManager.getApplication().getMessageBus()
                .syncPublisher(SettingsState.ON_SETTING_CHANGED);
        publisher.on();
    }

    @Override
    public void reset() {
        SettingsState settingsState = SettingsState.getInstance();
        settingComponent.setRenderServerRequestTypeText(settingsState.renderServerRequestType);
        settingComponent.setRenderServerText(settingsState.serverRendering);
        settingComponent.setIsExternalRenderBool(settingsState.renderIsExternal);
        settingComponent.setUsingModeText(settingsState.usingMode);
        settingComponent.setEnterprisePortalText(settingsState.enterprisePortal);
        settingComponent.setGitServerModeText(settingsState.gitServerMode);
        settingComponent.setGitPersonalToken(settingsState.gitPersonalToken);
        settingComponent.setGitServerText(settingsState.gitServer);
    }

    @Override
    public void disposeUIResources() {
        settingComponent = null;
    }
}
