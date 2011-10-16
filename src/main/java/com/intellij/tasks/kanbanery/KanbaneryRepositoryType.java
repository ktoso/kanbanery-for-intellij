package com.intellij.tasks.kanbanery;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.tasks.config.TaskRepositoryEditor;
import com.intellij.tasks.impl.BaseRepositoryType;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Konrad Malawski
 */
public class KanbaneryRepositoryType extends BaseRepositoryType<KanbaneryRepository> {

  public KanbaneryRepositoryType() {
  }

  @NotNull
  public String getName() {
    return "Kanbanery";
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return IconLoader.getIcon("/resources/kanbanery.png");
  }

  @NotNull
  @Override
  public KanbaneryRepository createRepository() {
    return new KanbaneryRepository(this);
  }

  @NotNull
  @Override
  public Class<KanbaneryRepository> getRepositoryClass() {
    return KanbaneryRepository.class;
  }

  @Override
  protected int getFeatures() {
    return BASIC_HTTP_AUTHORIZATION;
  }

  @NotNull
  @Override
  public TaskRepositoryEditor createEditor(KanbaneryRepository repository, Project project, Consumer<KanbaneryRepository> changeListener) {
    return new KanbaneryRepositoryEditor(project, repository, changeListener);
  }

}

