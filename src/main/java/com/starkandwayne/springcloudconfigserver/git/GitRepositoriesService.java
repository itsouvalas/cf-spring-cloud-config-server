package com.starkandwayne.springcloudconfigserver.git;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.jgit.api.Git;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.config.server.environment.JGitEnvironmentRepository;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GitRepositoriesService {
   private final List<GitRepositoriesService.Repository> repositories;
   private List<JGitEnvironmentRepository> environmentRepositories;

   @Autowired
   public GitRepositoriesService(Environment environment, List<JGitEnvironmentRepository> environmentRepositories) {
      this.environmentRepositories = environmentRepositories;
      BindResult<List<GitRepositoriesService.Repository>> compositeBinder = Binder.get(environment).bind("spring.cloud.config.server.composite", Bindable.listOf(Repository.class));
      BindResult<GitRepositoriesService.Repository> gitBinder = Binder.get(environment).bind("spring.cloud.config.server.git", Repository.class);
      if (compositeBinder.isBound()) {
         this.repositories = (List<GitRepositoriesService.Repository>)compositeBinder.get();
      } else if (gitBinder.isBound()) {
         this.repositories = Collections.singletonList(gitBinder.get());
      } else {
         this.repositories = Collections.emptyList();
      }

   }

   public List<?> getDefaultBranchCommitIds() {
     
      return (List<?>)this.repositories.stream().filter((repo) -> {
         return "git".equals(((GitRepositoriesService.Repository) repo).getType());
      }).map(this::snapshotRepository).collect(Collectors.toList());

    
   }

   private RepositorySnapshot snapshotRepository(Object obrepo) {
      Repository repo = (Repository)obrepo;
      String label;
      if (StringUtils.hasText(repo.getLabel())) {
         label = repo.getLabel();
      } else {
         label = "master";
      }

      this.forceCloneOnGitEnvironmentRepositories("application", "cloud", label);

      try {
         Git git = Git.open(new File(repo.getBaseDir()));
         Throwable var4 = null;

         RepositorySnapshot var5;
         try {
            var5 = new RepositorySnapshot(repo.getSourceUri(), label, git.getRepository().findRef(label).getObjectId().name());
         } catch (Throwable var15) {
            var4 = var15;
            throw var15;
         } finally {
            if (git != null) {
               if (var4 != null) {
                  try {
                     git.close();
                  } catch (Throwable var14) {
                     var4.addSuppressed(var14);
                  }
               } else {
                  git.close();
               }
            }

         }

         return var5;
      } catch (IOException var17) {
         return new RepositorySnapshot(repo.getSourceUri(), label, "n/a");
      }
   }

   private void forceCloneOnGitEnvironmentRepositories(String application, String profile, String label) {
      this.environmentRepositories.stream().filter((environmentRepository) -> {
         return environmentRepository instanceof JGitEnvironmentRepository;
      }).forEach((gitRepository) -> {
         try {
            JGitEnvironmentRepository jGitRepo = (JGitEnvironmentRepository)gitRepository;
            if (jGitRepo.getLastRefresh() == 0L) {
               jGitRepo.findOne(application, profile, label);
               jGitRepo.setLastRefresh(System.currentTimeMillis());
            }
         } catch (Exception var5) {
         }

      });
   }

   static class Repository {
      private String baseDir;
      private String label;
      private String sourceUri;
      private String type = "git";

      public String getBaseDir() {
         return this.baseDir;
      }

      public void setBaseDir(String baseDir) {
         this.baseDir = baseDir;
      }

      public String getLabel() {
         return this.label;
      }

      public void setLabel(String label) {
         this.label = label;
      }

      public String getSourceUri() {
         return this.sourceUri;
      }

      public void setSourceUri(String sourceUri) {
         this.sourceUri = sourceUri;
      }

      public String getType() {
         return this.type;
      }

      public void setType(String type) {
         this.type = type;
      }
   }

   public static class RepositorySnapshot {
      private final String sourceUri;
      private final String label;
      private final String commitId;

      public RepositorySnapshot(String sourceUri, String label, String commitId) {
         this.sourceUri = sourceUri;
         this.label = label;
         this.commitId = commitId;
      }

      public String getSourceUri() {
         return this.sourceUri;
      }

      public String getLabel() {
         return this.label;
      }

      public String getCommitId() {
         return this.commitId;
      }
   }
}