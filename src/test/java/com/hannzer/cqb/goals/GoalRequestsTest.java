package com.hannzer.cqb.goals;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** 目標與子任務的請求驗證：直接用 Bean Validation，不起 Spring */
@DisplayName("目標與子任務的請求驗證")
class GoalRequestsTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static List<String> messages(Object request) {
        return validator.validate(request).stream().map(ConstraintViolation::getMessage).toList();
    }

    @Nested
    @DisplayName("新增目標")
    class CreateGoal {

        @Test
        @DisplayName("標題去除前後空白；截止日轉成 LocalDate")
        void trimsAndParses() {
            var request = new GoalRequests.CreateGoal(1L, "  跑完半馬  ", "2027-10-31");

            assertThat(messages(request)).isEmpty();
            assertThat(request.title()).isEqualTo("跑完半馬");
            assertThat(request.dueDateValue()).isEqualTo(LocalDate.of(2027, 10, 31));
        }

        @Test
        @DisplayName("截止日選填")
        void dueDateIsOptional() {
            var request = new GoalRequests.CreateGoal(1L, "跑完半馬", null);

            assertThat(messages(request)).isEmpty();
            assertThat(request.dueDateValue()).isNull();
        }

        @ParameterizedTest(name = "「{0}」")
        @ValueSource(strings = {"", "2027/10/31", "2027-1-5", "2027-02-30", "2027-13-01", "明天"})
        @DisplayName("截止日格式錯誤或不是真實日期")
        void invalidDueDate(String dueDate) {
            assertThat(messages(new GoalRequests.CreateGoal(1L, "跑完半馬", dueDate)))
                    .containsExactly("預期截止日格式應為 YYYY-MM-DD");
        }

        @Test
        @DisplayName("標題必填、上限 200 字")
        void titleValidation() {
            assertThat(messages(new GoalRequests.CreateGoal(1L, "   ", null)))
                    .containsExactly("目標標題必填");
            assertThat(messages(new GoalRequests.CreateGoal(1L, null, null)))
                    .containsExactly("目標標題必填");
            assertThat(messages(new GoalRequests.CreateGoal(1L, "x".repeat(201), null)))
                    .containsExactly("目標標題不可超過 200 個字");
            assertThat(messages(new GoalRequests.CreateGoal(1L, "x".repeat(200), null))).isEmpty();
        }

        @Test
        @DisplayName("所屬領域必填且為正整數")
        void areaIdValidation() {
            assertThat(messages(new GoalRequests.CreateGoal(null, "跑完半馬", null)))
                    .containsExactly("所屬領域必填");
            assertThat(messages(new GoalRequests.CreateGoal(0L, "跑完半馬", null)))
                    .containsExactly("所屬領域 id 必須為正整數");
        }
    }

    @Nested
    @DisplayName("新增子任務")
    class CreateSubtask {

        @Test
        @DisplayName("標題去除前後空白、必填、上限 200 字")
        void titleValidation() {
            var request = new GoalRequests.CreateSubtask("  排定課表 ");
            assertThat(messages(request)).isEmpty();
            assertThat(request.title()).isEqualTo("排定課表");

            assertThat(messages(new GoalRequests.CreateSubtask(" "))).containsExactly("子任務標題必填");
            assertThat(messages(new GoalRequests.CreateSubtask("x".repeat(201))))
                    .containsExactly("子任務標題不可超過 200 個字");
        }
    }

    @Nested
    @DisplayName("修改子任務進度")
    class UpdateProgress {

        @Test
        @DisplayName("接受 JSON 數字 0／20／40／60／80／100")
        void validProgress() {
            for (int value : new int[] {0, 20, 40, 60, 80, 100}) {
                var request = new GoalRequests.UpdateProgress(value);
                assertThat(messages(request)).isEmpty();
                assertThat(request.progressValue()).isEqualTo(value);
            }
        }

        @Test
        @DisplayName("其他數值、字串、缺少都不接受")
        void invalidProgress() {
            for (Object value : new Object[] {50, -20, 120, "20", 20.5, null}) {
                assertThat(messages(new GoalRequests.UpdateProgress(value)))
                        .as("progress = %s", value)
                        .containsExactly("進度只能是 0、20、40、60、80、100");
            }
        }
    }
}
