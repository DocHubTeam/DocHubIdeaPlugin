package org.dochub.idea.arch.settings;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.*;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;

public class SettingComponent {
    private final JPanel myMainPanel;
    private final JPanel enterprisePanel;
    private final JPanel paramsPanel;
    private final JPanel gitServerPanel;
    private final JPanel gitSecurityPanel;
    private final DefaultComboBoxModel<String> modeModel = new DefaultComboBoxModel<>();
    private final DefaultComboBoxModel<String> renderServerRequestTypeModel = new DefaultComboBoxModel<>();
    private final ComboBox<String> usingMode = new ComboBox<>(modeModel);
    private final ComboBox<String> renderServerRequestType = new ComboBox<>(renderServerRequestTypeModel);
    private final JBTextField renderServer = new JBTextField();
    private final JBCheckBox isExternalRender = new JBCheckBox("External rendering");
    private final JBTextField enterprisePortal = new JBTextField();

    private final JBTextField personalToken = new JBTextField();
    private final JBTextField gitServer = new JBTextField();
    private final DefaultComboBoxModel<String> gitServerModes = new DefaultComboBoxModel<>();
    private final ComboBox<String> gitServerMode = new ComboBox<>(gitServerModes);

    public SettingComponent() {
        gitServerModes.addAll(Arrays.asList(SettingsState.gitServerModes));
        modeModel.addAll(Arrays.asList(SettingsState.modes));
        renderServerRequestTypeModel.addAll(Arrays.asList(SettingsState.renderServerRequestTypes));

        isExternalRender.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateVisible();
            }
        });

        usingMode.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateVisible();
            }
        });

        gitServerMode.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateVisible();
            }
        });

        enterprisePanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Enterprise portal: "), enterprisePortal, 1, false)
                .addComponent(new JBLabel("IMPORTANT: All settings will get from enterprise portal."), 1)
                .getPanel();

        paramsPanel = FormBuilder.createFormBuilder()
                .addComponent(new JSeparator(SwingConstants.HORIZONTAL))
                .addComponent(isExternalRender, 1)
                .addLabeledComponent(new JBLabel("Render server: "), renderServer, 1, false)
                .addLabeledComponent(new JBLabel("Render server request type: "), renderServerRequestType, 1, false)
                .getPanel();

        gitServerPanel = FormBuilder.createFormBuilder()
                .addComponent(new JSeparator(SwingConstants.HORIZONTAL))
                .addLabeledComponent(new JBLabel("Git server vendor: "), gitServerMode, 1, false)
                .addLabeledComponent(new JBLabel("Git server: "), gitServer, 1, false)
                .getPanel();

        gitSecurityPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Git personal token: "), personalToken, 1, false)
                .getPanel();

        myMainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Using mode: "), usingMode, 1, false)
                .addComponent(enterprisePanel, 1)
                .addComponent(paramsPanel, 1)
                .addComponent(gitServerPanel, 1)
                .addComponent(gitSecurityPanel, 1)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();

        updateVisible();
    }

    private void updateVisible() {
        if (usingMode.getSelectedIndex() == 1) {
            enterprisePanel.setVisible(true);
            paramsPanel.setVisible(false);
            gitServerPanel.setVisible(false);
            gitSecurityPanel.setVisible(true);
            personalToken.setEnabled(true);
        } else {
            enterprisePanel.setVisible(false);
            paramsPanel.setVisible(true);
            gitServerPanel.setVisible(true);
            gitServer.setEnabled(gitServerMode.getSelectedIndex() != 0);
            personalToken.setEnabled(gitServerMode.getSelectedIndex() != 0);
            renderServer.setEnabled(isExternalRender.isSelected());
            renderServerRequestType.setEnabled(isExternalRender.isSelected());
        }

    }

    public JPanel getPanel() {
        return myMainPanel;
    }

    public JComponent getPreferredFocusedComponent() {
        return isExternalRender;
    }

    public String getGitPersonalToken() {
        return personalToken.getText();
    }

    public void setGitPersonalToken(@NotNull String token) {
        personalToken.setText(token);
    }

    public String getGitServerText() {
        return gitServer.getText();
    }

    public void setGitServerText(@NotNull String url) {
        gitServer.setText(url);
   }

    public String getGitServerModeText() {
        return gitServerModes.getElementAt(gitServerMode.getSelectedIndex());
    }

    public void setGitServerModeText(@NotNull String mode) {
        int index = gitServerModes.getIndexOf(mode);
        if (index >= 0) gitServerMode.setSelectedIndex(index);
        updateVisible();
    }

    public String getUsingModeText() {
        return modeModel.getElementAt(usingMode.getSelectedIndex());
    }

    public void setUsingModeText(@NotNull String mode) {
        int index = modeModel.getIndexOf(mode);
        if (index >= 0) usingMode.setSelectedIndex(index);
        updateVisible();
    }

    @NotNull
    public String getRenderServerRequestTypeText() {
        return renderServerRequestTypeModel.getElementAt(renderServerRequestType.getSelectedIndex());
    }

    public void setRenderServerRequestTypeText(@NotNull String mode) {
        int index = renderServerRequestTypeModel.getIndexOf(mode);
        if (index >= 0) renderServerRequestType.setSelectedIndex(index);
        updateVisible();
    }

    public Boolean getIsExternalRenderBool() {
        return isExternalRender.isSelected();
    }

    public void setIsExternalRenderBool(Boolean value) {
        isExternalRender.setSelected(value);
        updateVisible();
    }

    @NotNull
    public String getRenderServerText() {
        return renderServer.getText();
    }

    public void setRenderServerText(@NotNull String server) {
        renderServer.setText(server);
        updateVisible();
    }

    public String getEnterprisePortalText() {
        return enterprisePortal.getText();
    }

    public void setEnterprisePortalText(@NotNull String portal) {
        enterprisePortal.setText(portal);
        updateVisible();
    }

}
