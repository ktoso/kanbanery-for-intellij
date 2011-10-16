package com.intellij.tasks.kanbanery;

import com.google.common.collect.Lists;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.tasks.Task;
import com.intellij.tasks.TaskState;
import com.intellij.tasks.impl.BaseRepositoryImpl;
import com.intellij.tasks.kanbanery.model.KanbaneryTask;
import com.intellij.util.xmlb.annotations.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.project13.janbanery.core.Janbanery;
import pl.project13.janbanery.core.JanbaneryFactory;
import pl.project13.janbanery.core.flow.TaskMarkFlow;
import pl.project13.janbanery.exceptions.ProjectNotFoundException;
import pl.project13.janbanery.resources.TaskType;
import pl.project13.janbanery.resources.User;
import pl.project13.janbanery.resources.Workspace;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newConcurrentMap;
import static com.google.common.collect.Maps.newHashMap;
import static com.intellij.openapi.util.text.StringUtil.isNotEmpty;

/**
 * @author Konrad Malawski
 */
@Tag("Kanbanery")
public class KanbaneryRepository extends BaseRepositoryImpl {

  private final static Logger LOG = Logger.getInstance("#com.intellij.tasks.kanbanery.KanbaneryRepository");

  private String myApiKey = "";
  private String myProjectName = "";
  private String myWorkspaceName = "";

  private Janbanery myJanbanery;

  private Map<Long, TaskType> myTaskTypeCache = newConcurrentMap();
  private Map<Long, User> myUsersCache = newConcurrentMap();

  /**
   * for serialization
   */
  @SuppressWarnings({"UnusedDeclaration"})
  public KanbaneryRepository() {
  }

  private KanbaneryRepository(KanbaneryRepository other) {
    super(other);
    this.myWorkspaceName = other.myWorkspaceName;
    this.myProjectName = other.myProjectName;
    this.myApiKey = other.myApiKey;
    this.myJanbanery = other.myJanbanery;
  }

  public KanbaneryRepository(KanbaneryRepositoryType type) {
    super(type);
  }

  @Override
  public Task[] getIssues(@Nullable String request, int max, long since) throws Exception {
    List<pl.project13.janbanery.resources.Task> all = janbanery().tasks().all();
    List<KanbaneryTask> tasks = Lists.transform(all, KanbaneryTask.transform(myTaskTypeCache));

    return tasks.toArray(new KanbaneryTask[tasks.size()]);
  }

  @Override
  public void setTaskState(Task task, TaskState state) throws Exception {
    pl.project13.janbanery.resources.Task target = new pl.project13.janbanery.resources.Task();
    target.setId(Long.valueOf(task.getId()));

    TaskMarkFlow mark = janbanery().tasks().mark(target);

    if (state == TaskState.IN_PROGRESS || state == TaskState.OPEN || state == TaskState.OTHER || state == TaskState.REOPENED) {
      mark.asNotReadyToPull();
    } else if (state == TaskState.RESOLVED) {
      mark.asReadyToPull();
    }
  }

  @Override
  public boolean isConfigured() {
    boolean hasCredentials = (isNotEmpty(myUsername) && isNotEmpty(myPassword)) || isNotEmpty(myApiKey);
    boolean hasProject = isNotEmpty(myWorkspaceName) && isNotEmpty(myProjectName);

    return hasCredentials && hasProject;
  }

  @NotNull
  @Override
  public String getPresentableName() {
    return "Kanbanery: " + myWorkspaceName + " / " + myProjectName;
  }

  @Nullable
  @Override
  public Task findTask(String id) {
    try {
      pl.project13.janbanery.resources.Task task = janbanery().tasks().byId(Long.parseLong(id));

      TaskType taskType = myTaskTypeCache.get(task.getTaskTypeId());

      return new KanbaneryTask(task, taskType);
    } catch (Exception e) {
      LOG.warn("Cannot get issue " + id + ": " + e.getMessage());
      return null;
    }
  }

  Janbanery janbanery() {
    LOG.info("Reloading Janbanery...");

    if (myJanbanery != null) {
      myJanbanery.close();
      myJanbanery = null;
    }

    try {
      JanbaneryFactory factory = new JanbaneryFactory();

      JanbaneryFactory.JanbaneryToWorkspace toWorkspace;
      if (hasApiKey()) {
        toWorkspace = factory.connectUsing(getApiKey());
      } else {
        toWorkspace = factory.connectUsing(getUsername(), getPassword());
      }

      if (myWorkspaceName.isEmpty() || myProjectName.isEmpty()) {
        myJanbanery = toWorkspace.notDeclaringWorkspaceYet();
      } else {
        try {
          myJanbanery = toWorkspace.toWorkspace(myWorkspaceName).usingProject(myProjectName);

          preloadTaskTypes();
          preloadUsers();

        } catch (ProjectNotFoundException ex) {
          myJanbanery = toWorkspace.toWorkspace(myWorkspaceName);
        }
      }

    } catch (Exception e) {
      LOG.warn("Unable to login to Kanbanery...", e);
      JOptionPane.showMessageDialog(null,
                                    "Please check your credentials and try again.",
                                    "Unable to login to Kanbanery.com",
                                    JOptionPane.WARNING_MESSAGE);
      return null;
    }

    return myJanbanery;
  }

  private void preloadUsers() {
    new SwingWorker<List<User>, Object>() {

      public List<User> all;

      @Override
      protected List<User> doInBackground() throws Exception {
        return all = myJanbanery.users().allWithNobody();
      }

      @Override
      protected void done() {
        HashMap<Long, User> users = newHashMap();
        for (User user : all) {
          users.put(user.getId(), user);
        }

        myUsersCache = users;
      }
    }.execute();
  }

  private void preloadTaskTypes() {
    new SwingWorker<List<TaskType>, Object>() {

      public List<TaskType> all;

      @Override
      protected List<TaskType> doInBackground() throws Exception {
        return all = myJanbanery.taskTypes().all();
      }

      @Override
      protected void done() {
        HashMap<Long, TaskType> tts = newHashMap();
        for (TaskType taskType : all) {
          tts.put(taskType.getId(), taskType);
        }

        myTaskTypeCache = tts;
      }
    }.execute();
  }

  public List<String> findDisplayableProjects() {
    List<Workspace> workspaces = getWorkspaces();
    List<String> displayableProjects = new ArrayList<String>();
    for (Workspace workspace : workspaces) {
      String workspaceName = workspace.getName();
      for (pl.project13.janbanery.resources.Project project : workspace.getProjects()) {
        String projectName = project.getName();

        displayableProjects.add(workspaceName + "/" + projectName);
      }
    }
    return displayableProjects;
  }

  @Override
  public KanbaneryRepository clone() {
    return new KanbaneryRepository(this);
  }

  public void setProject(String projectName) {
    this.myProjectName = projectName;
  }

  public void setWorkspaceName(String myWorkspaceName) {
    this.myWorkspaceName = myWorkspaceName;
  }

  public void reloadJanbanery() {
    closeJanbanery();

    // create an instance using the new credentials / workspace
    janbanery();
  }

  private void closeJanbanery() {
    if (this.myJanbanery != null) {
      myJanbanery.close();
      myJanbanery = null; // reset the lazy getter
    }
  }

  public boolean hasApiKey() {
    return !myApiKey.isEmpty();
  }

  @NotNull
  public String getApiKey() {
    return myApiKey;
  }

  /**
   * Set new credentials and return true if they have changed (a refresh of projects will be requirec)
   */
  public boolean newCredentials(String user, String pass) {
    boolean changed = false;
    if (notEqual(myUsername, user) || notEqual(myPassword, pass)) {
      changed = true;
    }

    myUsername = user;
    myPassword = pass;

    myApiKey = "";

    if (changed) {
      closeJanbanery();
    }

    LOG.info("Switched to username/pass auth in UI");
    return changed;
  }

  public void useApiKey(String apiKey) {
    myUsername = "";
    myPassword = "";

    if (notEqual(myApiKey, apiKey)) {
      closeJanbanery();
    }

    myApiKey = apiKey;
  }

  @SuppressWarnings("SimplifiableIfStatement")
  private boolean notEqual(String oldValue, String newValue) {
    if (oldValue == null && newValue == null) {
      return true;
    } else if (oldValue != null && newValue != null) {
      return !oldValue.equals(newValue);
    } else {
      return false;
    }
  }

  public List<Workspace> getWorkspaces() {
    return janbanery().workspaces().all();
  }

  @NotNull
  public String getSelectedItem() {
    return myWorkspaceName + "/" + myProjectName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    KanbaneryRepository that = (KanbaneryRepository) o;

    if (myProjectName != null ? !myProjectName.equals(that.myProjectName) : that.myProjectName != null) {
      return false;
    }
    if (myWorkspaceName != null ? !myWorkspaceName.equals(that.myWorkspaceName) : that.myWorkspaceName != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = myProjectName != null ? myProjectName.hashCode() : 0;
    result = 31 * result + (myWorkspaceName != null ? myWorkspaceName.hashCode() : 0);
    return result;
  }
}
