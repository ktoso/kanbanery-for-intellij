/*
 * Copyright 2000-2010 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.tasks.kanbanery;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.tasks.config.TaskRepositoryEditor;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.DocumentAdapter;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public class KanbaneryRepositoryEditor extends TaskRepositoryEditor {

  private final static Logger LOG = Logger.getInstance("#com.intellij.tasks.kanbanery.KanbaneryRepositoryEditor");

  // used as label in list of task servers
  protected JTextField myUrl;

  protected JTextField myUsernameText;
  protected JPasswordField myPasswordText;
  protected JTextField myApiKeyText;

  protected JComboBox myProjectsComboBox;
  protected JCheckBox myUseApiKeyCheckBox;
  protected JButton myRefreshButton;
  private JPanel myPanel;
  private JLabel myNeedsRefresh;

  private boolean myApplying;
  protected final KanbaneryRepository myRepository;
  private final Consumer<KanbaneryRepository> myChangeListener;

  public KanbaneryRepositoryEditor(final Project project, final KanbaneryRepository repository, Consumer<KanbaneryRepository> changeListener) {
    myRepository = repository;
    myChangeListener = changeListener;

    myUrl = new JTextField();
    myUrl.setText("Kanbanery: ");

    myRefreshButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
//        TaskManager.getManager(project).testConnection(repository);
        myRepository.reloadJanbanery();
      }
    });

    myNeedsRefresh.setVisible(false);

    if (repository.hasApiKey()) {
      myUseApiKeyCheckBox.setSelected(true);
      displayOnlyApiKey(repository);
    } else {
      myUseApiKeyCheckBox.setSelected(false);
      displayOnlyUsernameAndPass(repository);
    }

    myRefreshButton.addActionListener(new ReloadJanbaneryActionListener());

    myUseApiKeyCheckBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        boolean useApiKey = myUseApiKeyCheckBox.isSelected();
        if (useApiKey) {
          displayOnlyApiKey(repository);
        } else {
          displayOnlyUsernameAndPass(repository);
        }
      }
    });

    // reload janbanery and workspaces in UI
    try {
      new ReloadJanbaneryActionListener().actionPerformed(null);
      myProjectsComboBox.setSelectedItem(repository.getSelectedItem());
    } catch (Exception ignore) {
      // i don't care if it failed here
    }

    installListener(myUsernameText);
    installListener(myPasswordText);
    installListener(myUseApiKeyCheckBox);
    installListener(myApiKeyText);
    installListener(myProjectsComboBox);
  }

  private void displayOnlyUsernameAndPass(KanbaneryRepository repository) {
    myApiKeyText.setText("");
    myApiKeyText.setEnabled(false);

    myUsernameText.setText(repository.getUsername());
    myUsernameText.setEnabled(true);

    myPasswordText.setText(repository.getPassword());
    myPasswordText.setEnabled(true);
  }

  private void displayOnlyApiKey(KanbaneryRepository repository) {
    myApiKeyText.setText(repository.getApiKey());
    myApiKeyText.setEnabled(true);

    myUsernameText.setText("");
    myUsernameText.setEnabled(false);

    myPasswordText.setText("");
    myPasswordText.setEnabled(false);
  }

  private void installListener(JComboBox comboBox) {
    comboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doApply();
      }
    });
  }

  protected void installListener(JCheckBox checkBox) {
    checkBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        doApply();
      }
    });
  }

  protected void installListener(JTextField textField) {
    textField.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          public void run() {
            doApply();
          }
        });
      }
    });
  }

  private void doApply() {
    if (!myApplying) {
      try {
        myApplying = true;
        apply();
      } finally {
        myApplying = false;
      }
    }
  }

  public JComponent createComponent() {
    return myPanel;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myUsernameText;
  }

  public void apply() {
    // use api key
    if (myUseApiKeyCheckBox.isSelected()) {
      String apiKey = myApiKeyText.getText().trim();
      myRepository.useApiKey(apiKey);
      myNeedsRefresh.setVisible(true);
    } else {
      @SuppressWarnings("deprecation")
      String pass = myPasswordText.getText().trim();
      String user = myUsernameText.getText().trim();

      // use new credentials
      if (myRepository.newCredentials(user, pass)) {
        myNeedsRefresh.setVisible(true);
      }
    }

    String selectedWorkspaceAndProject = setupWorkspaceAndProject();

    myUrl.setText("Kanbanery: " + selectedWorkspaceAndProject);

    myChangeListener.consume(myRepository);
  }

  private String setupWorkspaceAndProject() {
    try {
      String selectedWorkspaceAndProject = (String) myProjectsComboBox.getSelectedItem();
      String[] split = selectedWorkspaceAndProject.split("/");
      String workspaceName = split[0];
      String projectName = split[1];
      myRepository.setWorkspaceName(workspaceName);
      myRepository.setProject(projectName);

      return selectedWorkspaceAndProject;
    } catch (NullPointerException e) {
      // ignore it
    }

    return "";
  }

  private class ReloadJanbaneryActionListener implements ActionListener {
    @Override
    public void actionPerformed(@Nullable ActionEvent e) {
      myNeedsRefresh.setVisible(false);

//      Object[] options = {"Cancel"};
//      JOptionPane optionPane = new JOptionPane("Connecting to Kanbanery...",
//                                               JOptionPane.DEFAULT_OPTION,
//                                               JOptionPane.INFORMATION_MESSAGE,
//                                               IconLoader.getIcon("/resources/kanbanery.png"),
//                                               options,
//                                               options[0]);
//      final JDialog dialog = optionPane.createDialog(myRefreshButton, "Please wait");
//      dialog.setVisible(true);
//
//      SwingWorker worker = new SwingWorker() {
//
//        @Override
//        protected Object doInBackground() throws Exception {
      LOG.info("Reloading kanbanery...");
      myRepository.reloadJanbanery();
      LOG.info("Done reloading!");
//          return null;
//        }
//
//        @Override
//        protected void done() {
//          dialog.setVisible(false);
//
      List<String> displayableProjects = myRepository.findDisplayableProjects();

      CollectionComboBoxModel model = new CollectionComboBoxModel(displayableProjects, displayableProjects.get(0));
      myProjectsComboBox.setModel(model);
//        }
//      };
//      worker.execute();
    }
  }
}