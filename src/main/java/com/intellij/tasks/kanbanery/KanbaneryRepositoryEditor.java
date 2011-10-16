/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import com.intellij.openapi.project.Project;
import com.intellij.tasks.config.BaseRepositoryEditor;
import com.intellij.util.Consumer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Dennis.Ushakov
 */
public class KanbaneryRepositoryEditor extends BaseRepositoryEditor<KanbaneryRepository> {

  private final JComboBox myWorkspace;
  private final JComboBox myProject;

  private final JTextField myWorkspaceName;
  private final JTextField myProjectName;

  public KanbaneryRepositoryEditor(final Project project,
                                   final KanbaneryRepository repository,
                                   Consumer<KanbaneryRepository> changeListener) {
    super(project, repository, changeListener);

    // used for keeping data
    myProjectName = new JTextField();
    myProjectName.setVisible(false);

    // used for keeping data
    myWorkspaceName = new JTextField();
    myWorkspaceName.setVisible(false);

    myUrlLabel.setVisible(false);
    myURLText.setVisible(false);

    myWorkspace = new JComboBox();
    initWorkspace(myWorkspace);

    myProject = new JComboBox();
    initProject(myProject);
  }

  private void initWorkspace(final JComboBox myWorkspace) {
    installListener(myWorkspaceName);

    myWorkspace.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        String workspace = myWorkspace.getSelectedItem().toString();
        myWorkspaceName.setText(workspace);
      }
    });

    myCustomPanel.add(myWorkspace, BorderLayout.NORTH);
    myCustomLabel.add(new JLabel("Workspace:", SwingConstants.RIGHT) {
      @Override
      public Dimension getPreferredSize() {
        final Dimension oldSize = super.getPreferredSize();
        final Dimension size = myWorkspace.getPreferredSize();
        return new Dimension(oldSize.width, size.height);
      }
    }, BorderLayout.NORTH);
  }

  private void initProject(final JComboBox myProject) {
    installListener(myProjectName);

    myProject.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        String project = myProject.getSelectedItem().toString();
        myProjectName.setText(project);
      }
    });

    myCustomPanel.add(myProject, BorderLayout.CENTER);
    myCustomLabel.add(new JLabel("Project:", SwingConstants.RIGHT) {
      @Override
      public Dimension getPreferredSize() {
        final Dimension oldSize = super.getPreferredSize();
        final Dimension size = myProject.getPreferredSize();
        return new Dimension(oldSize.width, size.height);
      }
    }, BorderLayout.CENTER);
  }

  @Override
  public void apply() {
    myRepository.setProject(myProjectName.getText().trim());
    myRepository.setWorkspaceName(myWorkspaceName.getText().trim());

    super.apply();

    myRepository.reloadJanbanery();
  }
}
