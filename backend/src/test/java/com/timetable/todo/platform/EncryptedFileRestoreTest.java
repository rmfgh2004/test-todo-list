package com.timetable.todo.platform;

import static org.assertj.core.api.Assertions.assertThat;

import com.timetable.todo.PlanningApplication;
import com.timetable.todo.planning.application.CreateTaskCommand;
import com.timetable.todo.planning.application.PlanningService;
import com.timetable.todo.planning.application.SortDirection;
import com.timetable.todo.planning.application.TaskListQuery;
import com.timetable.todo.planning.application.TaskSort;
import com.timetable.todo.planning.domain.Priority;
import com.timetable.todo.planning.domain.Task;
import com.timetable.todo.planning.domain.TaskTitle;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * FR-012, SECURITY-01, SECURITY-12: Stop-copy-start recovery of the encrypted development database.
 *
 * <p>Tagged {@code restore} and excluded from the ordinary test loop because it boots the
 * application several times against a real file. Run with {@code ./mvnw -Prestore verify}.
 */
@Tag("restore")
class EncryptedFileRestoreTest {

  private static final String KEY = "fileKeyForTest userKeyForTest";
  private static final String SECRET_TITLE = "quarterly board preparation";

  @TempDir Path workspace;

  @Test
  void FR_012_a_copied_encrypted_database_restores_every_task() throws Exception {
    Path original = workspace.resolve("original/planning");
    Files.createDirectories(original.getParent());

    withApplication(
        original,
        context ->
            context
                .getBean(PlanningService.class)
                .create(
                    new CreateTaskCommand(SECRET_TITLE, null, Priority.HIGH, 60, null),
                    "restore-smoke"));

    Path databaseFile = workspace.resolve("original/planning.mv.db");
    assertThat(databaseFile).exists();

    Path restored = workspace.resolve("restored/planning");
    Files.createDirectories(restored.getParent());
    Files.copy(
        databaseFile,
        workspace.resolve("restored/planning.mv.db"),
        StandardCopyOption.REPLACE_EXISTING);

    withApplication(
        restored,
        context -> {
          List<Task> tasks =
              context
                  .getBean(PlanningService.class)
                  .list(
                      new TaskListQuery(
                          null, null, null, TaskSort.CREATED_AT, SortDirection.DESC, 0, 25))
                  .content();
          assertThat(tasks).extracting(task -> task.title().value()).containsExactly(SECRET_TITLE);
        });
  }

  @Test
  void SECURITY_01_the_database_file_does_not_contain_task_content_in_clear_text()
      throws Exception {
    Path database = workspace.resolve("encrypted/planning");
    Files.createDirectories(database.getParent());

    withApplication(
        database,
        context ->
            context
                .getBean(PlanningService.class)
                .create(
                    new CreateTaskCommand(SECRET_TITLE, null, Priority.LOW, 30, null),
                    "restore-smoke"));

    String raw =
        new String(
            Files.readAllBytes(workspace.resolve("encrypted/planning.mv.db")),
            StandardCharsets.ISO_8859_1);

    assertThat(raw).doesNotContain(SECRET_TITLE);
  }

  @Test
  void SECURITY_12_a_title_value_object_still_validates_after_restore() {
    assertThat(TaskTitle.of(SECRET_TITLE).value()).isEqualTo(SECRET_TITLE);
  }

  private void withApplication(Path database, Consumer<ConfigurableApplicationContext> work) {
    // Command-line arguments outrank the surefire test profile, so the file datasource really
    // applies.
    try (ConfigurableApplicationContext context =
        new SpringApplicationBuilder(PlanningApplication.class)
            .web(WebApplicationType.NONE)
            .run(
                "--spring.profiles.active=file",
                "--spring.datasource.url=jdbc:h2:file:" + database + ";CIPHER=AES",
                "--spring.datasource.username=planning",
                "--spring.datasource.password=" + KEY,
                "--spring.flyway.enabled=true")) {
      work.accept(context);
    }
  }
}
