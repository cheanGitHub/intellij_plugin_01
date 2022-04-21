package com.cc.intellij.plugin.editgitexclude;

import com.google.common.collect.Sets;
import com.intellij.lang.jvm.JvmMethod;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PackageScope;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultElement;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TestDialogA extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JLabel label1;
    private JLabel label2;
    private JTextField textField1;
    private JTextField textField2;

    private Project project;

    public TestDialogA(PsiFile psiFile) {
//                NotificationGroupManager.getInstance()
//                        .getNotificationGroup("Custom Notification Group")
//                        .createNotification("aa", NotificationType.ERROR)
//                        .notify(project);

        NotificationGroup notificationGroup = new NotificationGroup("testid", NotificationDisplayType.BALLOON, false);
        Notification notification = notificationGroup.createNotification("测试通知", MessageType.INFO);
        Notifications.Bus.notify(notification, psiFile.getProject());


        setContentPane(contentPane);
        setModalityType(Dialog.DEFAULT_MODALITY_TYPE);
        getRootPane().setDefaultButton(buttonOK);
        setTitle("TestDialogA");

        setSize(500, 300);
        setLocation(400, 600);

        initUI(psiFile);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void initUI(PsiFile psiFile) {
        try {
            project = psiFile.getProject();
            String path = psiFile.getVirtualFile().getCanonicalPath(); // contentsToByteArray();

            SAXReader saxReader = new SAXReader();
            Document document = saxReader.read(new File(path));

            Element rootElement = document.getRootElement();
            if (!"mapper".equals(rootElement.getQualifiedName())) {
                return;
            }
            String namespace = rootElement.attribute("namespace").getValue();
            String javaPackage = namespace.substring(0, namespace.lastIndexOf("."));
            String javaFileName = namespace + ".java";
            // psiFile.getProject().
            // com.intellij.psi.search.FilenameIndex.get // PsiShortNamesCache#getFilesByName
//            JavaElementVisitor visitor = new JavaElementVisitor() {
//                public void visitMethodCallExpression(PsiMethodCallExpression expression) {
//                    super.visitMethodCallExpression(expression);
//                }
//            };
//            JavaUMethod method = new JavaUMethod();
//            method.accept(visitor);
//            AbstractBaseJavaLocalInspectionTool
            //LocalInspectionTool.

            // get scope
            final PsiPackage psiPackage = JavaPsiFacade.getInstance(project).findPackage(javaPackage);
            final GlobalSearchScope scope = PackageScope.packageScope(psiPackage, false);
            // final GlobalSearchScope scope = GlobalSearchScope.moduleRuntimeScope(module, false);
            // GlobalSearchScope scope = GlobalSearchScope.projectScope(project);

            // get PsiFile
//            @NotNull PsiFile[] psiFiles = FilenameIndex.getFilesByName(project, /*javaFileName*/"UserMapper.java", scope);
//            for (PsiFile file : psiFiles) {
//                file.accept(new JavaRecursiveElementVisitor() {
//                    @Override
//                    public void visitLocalVariable(PsiLocalVariable variable) {
//                        super.visitLocalVariable(variable);
//                        System.out.println("Found a variable at offset " + variable.getTextRange().getStartOffset());
//                    }
//                });
//            }

            // get PsiClass
            final PsiClass psiClass = getPsiClass(project, namespace); // JavaPsiFacade.getInstance(project).findClass(namespace, scope);

            // 解析 PsiClass
            // psiClass.getMethods()[0].getParameterList().getParameters()[0].getAnnotations()[0].findAttributeValue("value").getText(); // @Param注解的value的值
            Map<String, PsiMethod> methodMap = Arrays.stream(psiClass.getMethods()).collect(Collectors.toMap(PsiMethod::getName, Function.identity()));
            Iterator iterator = rootElement.elementIterator();
            while (iterator.hasNext()) {
                Element element = (Element) iterator.next();
                checkElement(element, methodMap);
            }
        } catch (Exception e) {
            e.printStackTrace();
            new CheckFailedTipsDialog(
                    psiFile.getVirtualFile().getCanonicalPath() + ":\n" +
                            "\t" + e.getMessage()).setVisible(true);
        }
    }

    private PsiClass getPsiClass(Project project, String className) {
        GlobalSearchScope scope = GlobalSearchScope.allScope(project); // projectScope(project);
        return JavaPsiFacade.getInstance(project).findClass(className, scope);
    }

    private static final Set<String> QUALIFIED_NAMES = Sets.newHashSet("select", "update", "insert", "delete");

    private void checkElement(Element element, Map<String, PsiMethod> methodMap) throws Exception {
        if (!QUALIFIED_NAMES.contains(element.getQualifiedName())) {
            return;
        }

        String methodName = element.attribute("id").getValue();
        PsiMethod psiMethod = methodMap.get(methodName);
        // JvmParameter[] jvmParameters = psiMethod.getParameters();
        PsiParameter[] parameters = psiMethod.getParameterList().getParameters();
        HashMap<String, PsiClass> paramAndClassMap = new HashMap<>();
        for (PsiParameter psiParameter : parameters) {
            String paramName = psiParameter.getName();
            PsiAnnotation[] annotations = psiParameter.getAnnotations();
            if (annotations.length > 0) {
                paramName = annotations[0].findAttributeValue("value").getText().replaceAll("\"", "");// @Param注解的value的值
            }
            String className = psiParameter.getType().getCanonicalText();

            // Class<?> psiClass = Class.forName(className);
            // PsiClassType type = (PsiClassType) psiParameter.getType().getDeepComponentType();
            PsiClass psiClass = getPsiClass(project, className);
            paramAndClassMap.put(paramName, psiClass);
        }

        check(element, paramAndClassMap);
    }

    private void check(Element element, HashMap<String, PsiClass> paramAndClassMap) {
        String qualifiedName = element.getQualifiedName();
        if ("select".equals(qualifiedName)) {
            checkSelect(element, paramAndClassMap);
        } else if ("where".equals(qualifiedName)) {
            checkWhere(element, paramAndClassMap);
        } else if ("if".equals(qualifiedName)) {
            checkIf(element, paramAndClassMap);
        } else if ("foreach".equals(qualifiedName)) {
            checkForeach(element, paramAndClassMap);
        } else {
            return;
        }

        // 解析子Tag
        for (Object obj : element.content()) {
            if (obj instanceof DefaultElement) {
                DefaultElement childElement = (DefaultElement) obj;
                check(childElement, paramAndClassMap);
            }
        }
    }

    private void checkSelect(Element element, HashMap<String, PsiClass> paramAndClassMap) {
        checkExpression(element.getTextTrim(), paramAndClassMap);
    }

    private void checkWhere(Element element, HashMap<String, PsiClass> paramAndClassMap) {
    }

    private void checkIf(Element element, HashMap<String, PsiClass> paramAndClassMap) {
        String value = element.attribute("test").getText().trim();
        for (String s : value.split("and|or")) {
            checkExpression("#{" + s.split("=|!=|>|<")[0].trim() + "}", paramAndClassMap);
        }

        checkExpression(element.getTextTrim(), paramAndClassMap);
    }

    private void checkForeach(Element element, HashMap<String, PsiClass> paramAndClassMap) {
        String value = element.attribute("collection").getValue().trim();
        PsiClass itemPsiClass = checkExpression("#{" + value + "}", paramAndClassMap);

        value = element.attribute("item").getValue().trim();
        paramAndClassMap.put(value, itemPsiClass);

        checkExpression(element.getTextTrim(), paramAndClassMap);
        paramAndClassMap.remove(value);
    }

    public static void main(String[] args) {
        for (String s : "user.item.id != null or user.item.id != ''".split("and|or")) {
            System.out.println(Arrays.toString(s.split("=|!=|>|<")));
        }
    }

    private PsiClass checkExpression(String data, HashMap<String, PsiClass> paramAndClassMap) {
        // String data = element.getTextTrim();
        Pattern p = Pattern.compile("#\\{(.*)}");
        Matcher m = p.matcher(data);
        while (m.find()) {
            String paramStr = m.group(1);
            System.out.println("paramStr = " + paramStr);

            String[] fields = paramStr.split("\\.");

            PsiClass returnType = paramAndClassMap.get(fields[0]);
            if (returnType == null) {
                throw new RuntimeException("mapper has no param named [" + fields[0] + "]");
            }

            for (int i = 1; i < fields.length; i++) {
                String field = fields[i];
                String getMethodName = "get" + field.toUpperCase(Locale.ENGLISH).substring(0, 1) + field.substring(1);
                JvmMethod getMethod = null;
                try {
                    JvmMethod[] methods = returnType.findMethodsByName(getMethodName);
                    if (methods.length < 1) {
                        throw new RuntimeException("param [" + fields[0] + "] has no field named [" + fields[i] + "]");
                    }
                    getMethod = methods[0];
                } catch (Exception ee) {
                    throw new RuntimeException("param [" + fields[0] + "] get field named [" + fields[i] + "] failed:", ee);
                }

                PsiClassReferenceType referenceType = (PsiClassReferenceType) getMethod.getReturnType();
                returnType = referenceType.resolve();
            }

            return returnType;
        }

        return null;
    }

    private void onOK() {
        // add your code here
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

//    public static void main(String[] args) {
//        TestDialogA dialog = new TestDialogA(project);
//        dialog.pack();
//        dialog.setVisible(true);
//        System.exit(0);
//    }
}
