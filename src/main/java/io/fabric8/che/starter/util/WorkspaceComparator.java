/*-
 * #%L
 * che-starter
 * %%
 * Copyright (C) 2017 Red Hat, Inc.
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */
package io.fabric8.che.starter.util;

import io.fabric8.che.starter.model.DevMachine;
import io.fabric8.che.starter.model.DevMachineRuntime;
import io.fabric8.che.starter.model.project.Project;
import io.fabric8.che.starter.model.project.Source;
import io.fabric8.che.starter.model.workspace.Workspace;
import io.fabric8.che.starter.model.workspace.WorkspaceCommand;
import io.fabric8.che.starter.model.workspace.WorkspaceCommandAttributes;
import io.fabric8.che.starter.model.workspace.WorkspaceConfig;
import io.fabric8.che.starter.model.workspace.WorkspaceEnvironment;
import io.fabric8.che.starter.model.workspace.WorkspaceLink;
import io.fabric8.che.starter.model.workspace.WorkspaceMachine;
import io.fabric8.che.starter.model.workspace.WorkspaceMachineAttribute;
import io.fabric8.che.starter.model.workspace.WorkspaceRecipe;
import io.fabric8.che.starter.model.workspace.WorkspaceRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class WorkspaceComparator implements Comparator<Workspace> {
    private static final Logger LOG = LoggerFactory.getLogger(WorkspaceComparator.class);

    /**
     * Workspaces are equal if this method returns 0
     * Compares workspaces gradually through their elements, if some discrepancy is present
     * and it cannot be sorted, this method returns <Integer>.<MAX_VALUE>
     * @param workspace1 <Workspace> first workspace to compare
     * @param workspace2 <Workspace> workspace to compare against
     * @return relative position against compared workspace
     */
    @Override
    public int compare(Workspace workspace1, Workspace workspace2) {

        if (workspace1 == workspace2) {
            return 0;
        }

        if (workspace1 == null) {
            return -1;
        } else if (workspace2 == null) {
            return 1;
        }

        // Compare IDs
        String workspace1ID = workspace1.getId();
        String workspace2ID = workspace2.getId();
        if (workspace1ID != null && workspace2ID != null) {
            int workspaceIDsEqual = workspace1ID.compareTo(workspace2ID);
            if (workspaceIDsEqual != 0) return workspaceIDsEqual;
        } else if (workspace1ID != null && workspace2ID == null) {
            return 1;
        } else if (workspace1ID == null && workspace2ID != null) {
            return -1;
        }

        // Compare machine states
        String workspace1Status = workspace1.getStatus();
        String workspace2Status = workspace2.getStatus();
        if (workspace1Status != null && workspace2Status != null) {
            int workspaceStatusesEqual = workspace1Status.compareTo(workspace2Status);
            if (workspaceStatusesEqual != 0) return workspaceStatusesEqual;
        } else if (workspace1Status != null && workspace2Status == null) {
            return 1;
        } else if (workspace1Status == null && workspace2Status!= null) {
            return -1;
        }

        // Compare workspace links
        if (!compareWorkspaceLinks(workspace1, workspace2)) {
            return Integer.MAX_VALUE;
        }

        // Compare workspace runtimes
        Integer workspaceRuntimesEqual = compareWorkspaceRuntimes(workspace1, workspace2);
        if (workspaceRuntimesEqual != null) return workspaceRuntimesEqual;

        // Compare configs
        WorkspaceConfig workspace1Config = workspace1.getConfig();
        WorkspaceConfig workspace2Config = workspace2.getConfig();
        if (workspace1Config != null && workspace2Config != null) {
            if (!compareStrings(workspace1Config.getName(),workspace2Config.getName())) {
                LOG.info("Workspace config names not equal.");
                return Integer.MAX_VALUE;
            }
            if (!compareStrings(workspace1Config.getDefaultEnv(),workspace2Config.getDefaultEnv())) {
                LOG.info("Workspace config default envs not equal.");
                return Integer.MAX_VALUE;
            }
            if (!compareStrings(workspace1Config.getDescription(),workspace2Config.getDescription())) {
                LOG.info("Workspace config descriptions not equal.");
                return Integer.MAX_VALUE;
            }
            if (!compareWorkspaceConfigLinks(workspace1Config, workspace2Config)) {
                LOG.info("Workspace config links not equal.");
                return Integer.MAX_VALUE;
            }
            if (!compareWorkspaceConfigProjects(workspace1Config, workspace2Config)) {
                LOG.info("Workspace config projects not equal.");
                return Integer.MAX_VALUE;
            }
            if (!compareWorkspaceConfigCommands(workspace1Config, workspace2Config)) {
                LOG.info("Workspace config commands not equal.");
                return Integer.MAX_VALUE;
            }
            if (!compareWorkspaceConfigEnvironments(workspace1Config, workspace2Config)) {
                LOG.info("Workspace config environments not equal.");
                return Integer.MAX_VALUE;
            }
        } else if (workspace1Config != null && workspace2Config == null) {
            return 1;
        } else if (workspace1Config == null && workspace2Config != null) {
            return -1;
        }

        return 0;
    }

    private Integer compareWorkspaceRuntimes(Workspace workspace1, Workspace workspace2) {
        if (workspace1.getRuntime() != null && workspace2.getRuntime() != null) {
            WorkspaceRuntime workspace1Runtime = workspace1.getRuntime();
            WorkspaceRuntime workspace2Runtime = workspace2.getRuntime();
            boolean hasDevMachine1=true,hasDevMachine2=true;
            DevMachine workspace1Machine=null,workspace2Machine=null;
            try {
                workspace1Machine = workspace1Runtime.getDevMachine();
            } catch (NullPointerException e) {
                hasDevMachine1 = false;
            }
            try {
                workspace2Machine = workspace2Runtime.getDevMachine();
            } catch (NullPointerException e) {
                hasDevMachine2 = false;
            }
            if (hasDevMachine1 && !hasDevMachine2) {
                return 1;
            } else if (!hasDevMachine1 && hasDevMachine2) {
                return -1;
            } else if (hasDevMachine1 && hasDevMachine2) {
                boolean hasMachineRuntime1=true,hasMachineRuntime2=true;
                DevMachineRuntime workspace1MachineRuntime=null,workspace2MachineRuntime=null;
                try {
                    workspace1MachineRuntime = workspace1Machine.getRuntime();
                } catch (NullPointerException e) {
                    hasMachineRuntime1 = false;
                }
                try {
                    workspace2MachineRuntime = workspace2Machine.getRuntime();
                } catch (NullPointerException e) {
                    hasMachineRuntime2 = false;
                }
                if (hasMachineRuntime1 && !hasMachineRuntime2) {
                    return 1;
                } else if (!hasMachineRuntime1 && hasMachineRuntime2) {
                    return -1;
                } else if (hasMachineRuntime1 && hasMachineRuntime2) {
                    if (!compareWorkspaceMachinesRuntimes(workspace1MachineRuntime, workspace2MachineRuntime)) {
                        return Integer.MAX_VALUE;
                    }
                }
            }
        } else if (workspace1.getRuntime() != null && workspace2.getRuntime() == null) {
            return 1;
        } else if (workspace1.getRuntime() == null && workspace2.getRuntime() != null) {
            return -1;
        }
        return null;
    }

    private boolean compareWorkspaceMachinesRuntimes(DevMachineRuntime workspace1MachineRuntime, DevMachineRuntime workspace2MachineRuntime) {
        AtomicBoolean workspaceMachinesRuntimesEquals = new AtomicBoolean(true);
        workspace1MachineRuntime.getServers().forEach((key1, value1) -> {
            AtomicBoolean found = new AtomicBoolean(false);
            workspace2MachineRuntime.getServers().forEach((key2, value2) -> {
                if (key1.equals(key2) &&
                    compareStrings(value1.getRef(), value2.getRef()) &&
                    compareStrings(value1.getUrl(), value2.getUrl())
                ) {
                    found.set(true);
                }
            });
            if (!found.get()) {
                workspaceMachinesRuntimesEquals.set(false);
            }
        });
        return workspaceMachinesRuntimesEquals.get();
    }

    private boolean compareWorkspaceConfigEnvironments(WorkspaceConfig workspace1Config, WorkspaceConfig workspace2Config) {
        Map<String, WorkspaceEnvironment> workspace1Environments = workspace1Config.getEnvironments();
        Map<String, WorkspaceEnvironment> workspace2Environments = workspace2Config.getEnvironments();
        if (workspace1Environments != null && workspace2Environments != null) {
            WorkspaceEnvironment workspace1Environment = workspace1Environments.get(workspace1Config.getDefaultEnv());
            WorkspaceEnvironment workspace2Environment = workspace2Environments.get(workspace2Config.getDefaultEnv());
            if (workspace1Environment != null && workspace2Environment != null) {
                WorkspaceRecipe workspace1Recipe = workspace1Environment.getRecipe();
                WorkspaceRecipe workspace2Recipe = workspace2Environment.getRecipe();
                Map<String,WorkspaceMachine> workspace1Machines = workspace1Environment.getMachines();
                Map<String,WorkspaceMachine> workspace2Machines = workspace2Environment.getMachines();
                if (!compareWorkspaceRecipes(workspace1Recipe, workspace2Recipe)) {
                    return false;
                }
                if (!compareWorkspaceMachines(workspace1Machines, workspace2Machines)) {
                    return false;
                }
            } else if(workspace1Environment == null && workspace2Environment == null) {
                return true;
            } else {
                return false;
            }
        } else if (workspace1Environments == null && workspace2Environments == null) {
            return true;
        } else {
            return false;
        }
        return true;
    }

    private boolean compareWorkspaceMachines(Map<String, WorkspaceMachine> workspace1Machines, Map<String, WorkspaceMachine> workspace2Machines) {
        if (workspace1Machines != null && workspace2Machines != null) {
            AtomicBoolean workspaceMachinesEqual = new AtomicBoolean(true);
            workspace1Machines.forEach((key1, value1) -> {
                AtomicBoolean found = new AtomicBoolean(false);
                workspace2Machines.forEach((key2, value2) -> {
                    if (compareStrings(key1, key2)) {
                        if (value1.getAgents().containsAll(value2.getAgents()) &&
                            value1.getAgents().size() == value2.getAgents().size() &&
                            compareWorkspaceMachineAttributes(value1, value2)
                        ) {
                            found.set(true);
                        }
                    }
                    if (!found.get()) {
                        workspaceMachinesEqual.set(false);
                    }
                });
            });
            return workspaceMachinesEqual.get();
        } else if (workspace1Machines == null && workspace2Machines == null) {
            return true;
        }
        return false;
    }

    private boolean compareWorkspaceMachineAttributes(WorkspaceMachine workspace1Machine, WorkspaceMachine workspace2Machine) {
        WorkspaceMachineAttribute workspace1MachineAttributes = workspace1Machine.getAttributes();
        WorkspaceMachineAttribute workspace2MachineAttributes = workspace2Machine.getAttributes();
        if (workspace1MachineAttributes != null && workspace2MachineAttributes != null) {
            return compareStrings(workspace1MachineAttributes.getMemoryLimitBytes(),workspace2MachineAttributes.getMemoryLimitBytes());
        } else if (workspace1MachineAttributes == null && workspace2MachineAttributes == null) {
            return true;
        }
        return false;
    }

    private boolean compareWorkspaceRecipes(WorkspaceRecipe workspace1Recipe, WorkspaceRecipe workspace2Recipe) {
        if (workspace1Recipe != null && workspace2Recipe != null) {
            if (compareStrings(workspace1Recipe.getContent(), workspace2Recipe.getContent()) &&
                compareStrings(workspace1Recipe.getContentType(), workspace2Recipe.getContentType()) &&
                compareStrings(workspace1Recipe.getLocation(), workspace2Recipe.getLocation()) &&
                compareStrings(workspace1Recipe.getType(), workspace2Recipe.getType())
            ) {
                return true;
            }
        } else if (workspace1Recipe == null && workspace2Recipe == null) {
            return true;
        }
        return false;
    }

    private boolean compareWorkspaceConfigCommands(WorkspaceConfig workspace1Config, WorkspaceConfig workspace2Config) {
        List<WorkspaceCommand> workspace1Commands = workspace1Config.getCommands();
        List<WorkspaceCommand> workspace2Commands = workspace2Config.getCommands();
        if (workspace1Commands != null && workspace2Commands != null) {
            AtomicBoolean workspaceConfigCommandsEqual = new AtomicBoolean(true);
            workspace1Commands.forEach(workspace1ConfigCommand -> {
                AtomicBoolean found = new AtomicBoolean(false);
                workspace2Commands.forEach(workspace2ConfigCommand -> {
                    if (compareStrings(workspace1ConfigCommand.getCommandLine(), workspace2ConfigCommand.getCommandLine()) &&
                        compareStrings(workspace1ConfigCommand.getName(), workspace2ConfigCommand.getName()) &&
                        compareStrings(workspace1ConfigCommand.getType(), workspace2ConfigCommand.getType()) &&
                        compareWorkspaceConfigCommandsAttributes(workspace1ConfigCommand.getAttributes(), workspace2ConfigCommand.getAttributes())
                    ) {
                        found.set(true);
                    }
                });
                if (!found.get()) {
                    workspaceConfigCommandsEqual.set(false);
                }
            });
            return workspaceConfigCommandsEqual.get();
        } else if (workspace1Commands == null && workspace2Commands == null) {
            return true;
        }
        return false;
    }

    private boolean compareWorkspaceConfigCommandsAttributes(
            WorkspaceCommandAttributes workspace1ConfigCommandAttribute,
            WorkspaceCommandAttributes workspace2ConfigCommandAttribute)
    {
        return compareStrings(workspace1ConfigCommandAttribute.getGoal(),workspace2ConfigCommandAttribute.getGoal()) &&
               compareStrings(workspace1ConfigCommandAttribute.getPreviewUrl(),workspace2ConfigCommandAttribute.getPreviewUrl());
    }

    private boolean compareWorkspaceConfigProjects(WorkspaceConfig workspace1Config, WorkspaceConfig workspace2Config) {
        List<Project> workspace1Projects = workspace1Config.getProjects();
        List<Project> workspace2Projects = workspace2Config.getProjects();
        if (workspace1Projects != null && workspace2Projects != null) {
            AtomicBoolean workspaceConfigProjectsEqual = new AtomicBoolean(true);
            workspace1Projects.forEach(workspace1ConfigProject -> {
                AtomicBoolean found = new AtomicBoolean(false);
                workspace2Projects.forEach(workspace2ConfigProject -> {
                    if (compareStrings(workspace1ConfigProject.getDescription(), workspace2ConfigProject.getDescription()) &&
                        compareStrings(workspace1ConfigProject.getName(), workspace2ConfigProject.getName()) &&
                        compareStrings(workspace1ConfigProject.getPath(), workspace2ConfigProject.getPath()) &&
                        compareStrings(workspace1ConfigProject.getType(), workspace2ConfigProject.getType()) &&
                        workspace1ConfigProject.getMixins().containsAll(workspace2ConfigProject.getMixins()) &&
                        workspace1ConfigProject.getMixins().size() == workspace2ConfigProject.getMixins().size() &&
                        compareWorkspaceConfigProjectsSource(workspace1ConfigProject.getSource(), workspace2ConfigProject.getSource())
                    ) {
                        found.set(true);
                    }
                });
                if (!found.get()) {
                    workspaceConfigProjectsEqual.set(false);
                }
            });
            return workspaceConfigProjectsEqual.get();
        } else if (workspace1Projects == null && workspace2Projects == null) {
            return true;
        }
        return false;
    }

    private boolean compareWorkspaceConfigProjectsSource(Source workspace1ConfigProjectSource, Source workspace2ConfigProjectSource) {
        return workspace1ConfigProjectSource.getParameters().entrySet().containsAll(workspace2ConfigProjectSource.getParameters().entrySet()) &&
               compareStrings(workspace1ConfigProjectSource.getLocation(),workspace2ConfigProjectSource.getLocation()) &&
               compareStrings(workspace1ConfigProjectSource.getType(),workspace2ConfigProjectSource.getType());
    }

    private boolean compareWorkspaceConfigLinks(WorkspaceConfig workspace1Config, WorkspaceConfig workspace2Config) {
        List<WorkspaceLink> workspace1Links = workspace1Config.getLinks();
        List<WorkspaceLink> workspace2Links = workspace2Config.getLinks();
        if (workspace1Links != null && workspace2Links != null) {
            AtomicBoolean workspaceConfigLinksEqual = new AtomicBoolean(true);
            workspace1Links.forEach(workspace1ConfigLink -> {
                AtomicBoolean found = new AtomicBoolean(false);
                workspace2Links.forEach(workspace2ConfigLink -> {
                    if (compareStrings(workspace1ConfigLink.getMethod(), workspace2ConfigLink.getMethod()) &&
                        compareStrings(workspace1ConfigLink.getHref(), workspace2ConfigLink.getHref()) &&
                        compareStrings(workspace1ConfigLink.getRel(), workspace2ConfigLink.getRel())
                    ) {
                        found.set(true);
                    }
                });
                if (!found.get()) {
                    workspaceConfigLinksEqual.set(false);
                }
            });
            return workspaceConfigLinksEqual.get();
        } else if (workspace1Links == null && workspace2Links == null) {
            return true;
        }
        return false;
    }

    private boolean compareWorkspaceLinks(Workspace workspace1, Workspace workspace2) {
        List<WorkspaceLink> workspace1Links = workspace1.getLinks();
        List<WorkspaceLink> workspace2Links = workspace2.getLinks();
        if (workspace1Links != null && workspace2Links != null) {
            AtomicBoolean workspaceLinksEqual = new AtomicBoolean(true);
            workspace1Links.forEach(workspace1Link -> {
                AtomicBoolean found = new AtomicBoolean(false);
                workspace2Links.forEach(workspace2Link -> {
                    if (compareStrings(workspace1Link.getRel(),workspace2Link.getRel()) &&
                        compareStrings(workspace1Link.getHref(),workspace2Link.getHref()) &&
                        compareStrings(workspace1Link.getMethod(),workspace2Link.getMethod())
                    ) {
                        found.set(true);
                    }
                });
                if (!found.get()) {
                    workspaceLinksEqual.set(false);
                }
            });
            return workspaceLinksEqual.get();
        } else if (workspace1Links == null && workspace2Links == null) {
            return true;
        }
        return false;
    }

    //TODO: Find something better, default behavior missing
    private static boolean compareStrings(String s1, String s2) {
        return (s1 == null ? s2 == null : s1.equals(s2));
    }

}
