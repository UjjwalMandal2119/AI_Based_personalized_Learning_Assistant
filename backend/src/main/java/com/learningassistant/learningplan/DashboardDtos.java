package com.learningassistant.learningplan;

import com.learningassistant.learninggoal.LearningGoalDtos;
import com.learningassistant.task.TaskDtos;

import java.util.List;

public final class DashboardDtos {
    private DashboardDtos() {}

    public record DashboardResponse(String displayName, List<LearningGoalDtos.GoalResponse> activeGoals,
                                   long completedTasks, long remainingTasks,
                                   List<TaskDtos.TaskResponse> tasksDueToday,
                                   List<TaskDtos.TaskResponse> upcomingTasks,
                                   List<TaskDtos.TaskResponse> recentActivity) {}
}
