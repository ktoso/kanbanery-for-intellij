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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.tasks.TaskManager;
import com.intellij.tasks.config.TaskRepositoryEditor;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.DocumentAdapter;
import com.intellij.util.Consumer;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public class KanbaneryRepositoryEditor extends TaskRepositoryEditor {

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

    myRefreshButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        TaskManager.getManager(project).testConnection(repository);
      }
    });

    myNeedsRefresh.setVisible(false);

    if (repository.hasApiKey()) {
      myUseApiKeyCheckBox.setSelected(true);
    }
    myUsernameText.setText(repository.getLogin());
    myPasswordText.setText(repository.getPassword());

    myRefreshButton.addActionListener(new ReloadJanbaneryActionListener());

    installListener(myUsernameText);
    installListener(myPasswordText);
    installListener(myUseApiKeyCheckBox);

    enableButtons();
  }

  protected void enableButtons() {

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
    String user = myUsernameText.getText().trim();
    String pass = myPasswordText.getText().trim();

    if (myRepository.newCredentials(user, pass)) {
      myNeedsRefresh.setVisible(true);
    }

    if (myUseApiKeyCheckBox.isSelected()) {
      String apiKey = myApiKeyText.getText().trim();
      myRepository.useApiKey(apiKey);
    }

    myChangeListener.consume(myRepository);
  }

  private class ReloadJanbaneryActionListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      myNeedsRefresh.setVisible(false);

      Object[] options = {"Cancel"};
      JOptionPane optionPane = new JOptionPane("Connecting to Kanbanery...",
                                               JOptionPane.DEFAULT_OPTION,
                                               JOptionPane.INFORMATION_MESSAGE,
                                               IconLoader.getIcon("/resources/kanbanery.png"),
                                               options,
                                               options[0]);
      final JDialog dialog = optionPane.createDialog(myRefreshButton, "Please wait");

      SwingWorker worker = new SwingWorker() {

        @Override
        protected Object doInBackground() throws Exception {
          myRepository.reloadJanbanery();
          return null;
        }

        @Override
        protected void done() {
          dialog.setVisible(false);

          List<String> displayableProjects = myRepository.findDisplayableProjects();

          CollectionComboBoxModel model = new CollectionComboBoxModel(displayableProjects, displayableProjects.get(0));
          myProjectsComboBox.setModel(model);
        }
      };
      worker.execute();


    }
  }
}
