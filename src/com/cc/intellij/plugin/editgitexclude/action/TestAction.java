package com.cc.intellij.plugin.editgitexclude.action;

import com.cc.intellij.plugin.editgitexclude.TestDialogA;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

public class TestAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
//        NotificationGroup notificationGroup = new NotificationGroup("TestAction_displayId", NotificationDisplayType.BALLOON, false);
//        Notification notification = notificationGroup.createNotification("TestAction", MessageType.INFO);
//        Notifications.Bus.notify(notification);

        // Project project = e.getData(PlatformDataKeys.PROJECT);
        // Messages.showMessageDialog(project, "msg", "title", Messages.getInformationIcon());
        // Messages.dialog(project, "msg", "title", Messages.getInformationIcon());

        // Project project = e.getRequiredData(CommonDataKeys.PROJECT);
        PsiFile psiFile = e.getRequiredData(CommonDataKeys.PSI_FILE);
        TestDialogA dialog = new TestDialogA(psiFile);
        // dialog.setResizable(false); //是否允许用户通过拖拽的方式扩大或缩小你的表单框，我这里定义为true，表示允许
        dialog.setVisible(true);
    }
}
