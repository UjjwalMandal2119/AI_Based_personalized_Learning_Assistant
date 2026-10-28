# Disha AI — Product Wireframes

Low-fidelity, behavior-first wireframes for the complete learning flow. The layouts below are based on the current Angular routes and backend capabilities: account access, learning goals, study tasks, and generated/saved lessons. They are intended to guide the interface, not to promise features that do not exist yet.

## Product flow

```text
Landing page ──→ Register ──→ Create goal ──→ Goal detail ──→ Generate/open lesson
      └────────→ Sign in ───────────────────→ Dashboard ────→ Goal detail
                                                   └─────────→ Create goal
```

## 1. Landing page — `/`

Purpose: explain the learning assistant and make starting or resuming learning obvious.

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Disha AI                         How it works     Sign in [Get started]│
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  YOUR PERSONAL AI LEARNING COACH       ┌──────────────────────────┐ │
│  Turn your learning goals into a       │ Example learning plan    │ │
│  plan you can follow.                  │ Learn Java Collections   │ │
│                                         │ Progress: 2 of 4         │ │
│  Create a goal, organize study tasks,  │ ✓ Review ArrayList       │ │
│  and generate study material for it.   │ ✓ Learn HashMap           │ │
│                                         │ □ Complete study task    │ │
│  [Start learning →]  See how it works  │ □ Review a topic         │ │
│                                         └──────────────────────────┘ │
│                                                                      │
├──────────────────────────────────────────────────────────────────────┤
│ How it works:  1. Set a goal  →  2. Make a plan  →  3. Study & review│
├──────────────────────────────────────────────────────────────────────┤
│ Feature overview                         [Create your learning plan] │
└──────────────────────────────────────────────────────────────────────┘
```

The plan card is illustrative; do not imply that the example data belongs to the visitor. When signed in, primary calls to action should take the learner to the dashboard instead of registration.

## 2. Sign in and registration — `/login`, `/register`

Purpose: authenticate with the fewest distractions; registration collects only required account details.

```text
┌──────────────────────────────┬───────────────────────────────────────┐
│ Disha AI                     │                                       │
│                              │  Welcome back / Create your account  │
│  Your learning journey,      │  Sign in to see your learning plan.   │
│  made personal.              │                                       │
│                              │  Your name          [_____________]  │
│  Set a goal, build a steady  │  Email address      [_____________]  │
│  study habit, and see your   │  Password           [_____________]  │
│  progress come to life.      │                                       │
│                              │  [Sign in / Create account]           │
│                              │  New to Disha AI? Create an account   │
└──────────────────────────────┴───────────────────────────────────────┘
```

- Registration shows the name field; sign-in hides it.
- Put validation beside the affected field and service errors above the form.
- On narrow screens, stack the form first and hide decorative copy before reducing input width.
- Preserve visible keyboard focus and real labels; placeholders are examples, not labels.

## 3. Learning dashboard — `/dashboard`

Purpose: answer “What have I made progress on?” and “What should I work on next?”

```text
┌──────────────────────────────────────────────────────────────────────┐
│ Disha AI        My learning   New goal             Alex Morgan [Sign out]│
├──────────────────────────────────────────────────────────────────────┤
│ Today                                            [+ Create a goal]    │
│ Good to see you, Alex                                                 │
│ A focused session today brings you closer to what you want to achieve.│
│                                                                      │
│ ┌────────────────┐ ┌────────────────┐ ┌────────────────┐ ┌────────┐│
│ │ Active goals   │ │ Tasks completed│ │ Tasks remaining│ │Progress││
│ │       2        │ │       8        │ │       4        │ │  67%   ││
│ └────────────────┘ └────────────────┘ └────────────────┘ └────────┘│
│                                                                      │
│ ┌─────────────────────────────────────┐ ┌──────────────────────────┐│
│ │ Your learning goals                │ │ Today's focus             ││
│ │ [J] Java collections         →     │ │ □ Read the lesson         ││
│ │     Java · Collections              │ │   Study · 20 min          ││
│ │ [M] Linear equations         →     │ ├──────────────────────────┤│
│ │     Mathematics · Algebra           │ │ Coming up                 ││
│ ├─────────────────────────────────────┤ │ □ Practice examples      ││
│ │ Recent learning activity            │ │   Practice · Due date     ││
│ │ ✓ Review arrays · Java              │ └──────────────────────────┘│
│ └─────────────────────────────────────┘                             │
└──────────────────────────────────────────────────────────────────────┘
```

The counts and task lists must come from the dashboard response. Goal rows open goal detail; task checkboxes update completion. If there are no goals or tasks, show a helpful empty state and a create-goal action. Provide explicit loading, retry, and error states instead of a blank dashboard.

## 4. Create a learning goal — `/goals/new`

Purpose: capture a clear topic and learner level before creating the goal.

```text
┌──────────────────────────────────────────────────────────────────────┐
│ ← Back to your dashboard                                             │
│                                                                      │
│ ┌──────────────────────────────────────────────────────────────────┐ │
│ │ A plan with purpose                                               │ │
│ │ Create a learning goal                                            │ │
│ │ Start with one topic you’d like to understand a little better.    │ │
│ │                                                                  │ │
│ │ Goal title     [Understand the fundamentals of algebra_______]   │ │
│ │ Subject        [Mathematics________]  Topic [Linear equations__]  │ │
│ │ Current level  [Beginner       ▾]   Target date [Optional_____]   │ │
│ │                                                                  │ │
│ │                                  [Cancel] [Create goal]          │ │
│ └──────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────┘
```

Use inline validation, keep the target date optional, and disable submission while invalid or saving. Successful creation should open the new goal detail page.

## 5. Goal detail — `/goals/:goalId`

Purpose: give one goal a home for its learning material and actionable task plan.

```text
┌──────────────────────────────────────────────────────────────────────┐
│ ← Back to your dashboard                                             │
│ JAVA · COLLECTIONS                         [Edit goal] [Delete]      │
│ Learn Java Collections                                               │
│ Starting at beginner · Target date                                   │
│                                                                      │
│ ┌──────────────────────────────────────────────────────────────────┐ │
│ │ Personalized study material                      [Generate lesson]│ │
│ │ A lesson tailored to your current level.                          │ │
│ │ Saved lessons                                                     │ │
│ │ Learn Java Collections · 12 min · Oct 28                      →   │ │
│ └──────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│ ┌──────────────────────────────────────────────────────────────────┐ │
│ │ Add a study task                                                  │ │
│ │ Task title        [Review the first lesson____________________]   │ │
│ │ Notes             [Optional__________________________________]   │ │
│ │ Type [Study ▾] Minutes [20] Due date [Optional] [Add task]       │ │
│ └──────────────────────────────────────────────────────────────────┘ │
│ Study tasks                                           1 / 3 done     │
│ □ Review the first lesson     Study · 20 min · Due Oct 29            │
│ ✓ Read the overview           Review · Complete                     │
└──────────────────────────────────────────────────────────────────────┘
```

Edit goal fields and status in a clearly separated edit state. Confirm destructive deletion before proceeding. Show lesson generation progress and errors next to that action. Saved lessons open the lesson page; task completion remains reversible.

## 6. Lesson view — `/lessons/:lessonId`

Purpose: provide a calm, readable study surface for generated content.

```text
┌──────────────────────────────────────────────────────────────────────┐
│ ← Back to Java Collections                                           │
│                                                                      │
│ STUDY MATERIAL · BEGINNER                                            │
│ Learn Java Collections                                               │
│ Collections · 12 min read · Generated Oct 28                         │
│                                                                      │
│ ┌──────────────────────────────────────────────────────────────────┐ │
│ │ What you'll learn                                                 │ │
│ │ • Understand what a collection is                                 │ │
│ │ • Choose a collection for a basic use case                        │ │
│ └──────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│ 1. Collection basics                                                 │
│    Lesson explanation text with a comfortable reading measure.      │
│                                                                      │
│ Worked examples                                                     │
│ ┌──────────────────────────────────────────────────────────────────┐ │
│ │ Example title · Scenario · Worked solution                       │ │
│ └──────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│ Key takeaways                                                       │
│ • Summary point                                                     │
└──────────────────────────────────────────────────────────────────────┘
```

Keep generated text as plain, readable content. Use a single main reading column, clear section headings, and preserve whitespace; return to the owning goal with the back link.

## Responsive behavior

At tablet widths, reduce the dashboard metric columns and stack the secondary task panel below the goal/activity panel. At phone widths:

- Collapse the header navigation into a compact, accessible menu or wrap it without horizontal scrolling.
- Stack dashboard metrics into two columns, then one column on very narrow screens.
- Put dashboard panels and goal form fields into one column.
- Keep primary actions full-width when that improves touch access; preserve a visible back action.
- Allow lesson content to use the available width with comfortable side padding.

## Shared states and interaction rules

- Every data-backed screen has loading, empty, success, and recoverable error states.
- Keep success/error feedback adjacent to the action that caused it.
- Use semantic headings, buttons, links, form labels, and progress indicators; do not rely on color alone.
- Ensure all interactive controls are reachable and understandable by keyboard and screen reader.
- Confirm destructive actions and prevent duplicate submissions while requests are in progress.
- Do not show generated lessons, recommendations, quizzes, weak-area analysis, or learning-plan automation as live functionality unless the API supports them.

## Current implementation mapping

| Wireframe | Angular route | Existing capability |
|---|---|---|
| Landing | `/` | Product introduction and account calls to action |
| Sign in / registration | `/login`, `/register` | Account authentication |
| Dashboard | `/dashboard` | Goal/task counts, due/upcoming tasks, recent activity |
| Goal creation | `/goals/new` | Goal title, subject, topic, level, optional target date |
| Goal detail | `/goals/:goalId` | Goal editing, task management, generated/saved lessons |
| Lesson | `/lessons/:lessonId` | Saved generated lesson content |
