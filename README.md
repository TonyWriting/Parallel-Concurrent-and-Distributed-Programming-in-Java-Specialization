# [Coursera] Parallel, Concurrent, and Distributed Programming in Java Specialization

## Parallel Programming in Java

### Module 3: Functional Parallelism

#### Future Tasks

Future tasks are tasks with return values, and a future object is a “handle” for accessing a task’s return value. There are two key operations that can be performed on a future object, `A`: 

1. Assignment — `A` can be assigned a reference to a future object returned by a task of the form, *future* { ⟨ *task-with-return-value* ⟩ } (using pseudocode notation). The content of the future object is constrained to be *single assignment* (similar to a final variable in Java), and cannot be modified after the future task has returned.
2. Blocking read — the operation, `A.get()`, waits until the task associated with future object `A` has completed, and then propagates the task’s return value as the value returned by `A.get()`.  Any statement, S, executed after `A.get()` can be assured that the task associated with future object A must have completed before S starts execution.

#### Creating Future Tasks in Java’s Fork/Join Framework

We can express future tasks in Java’s Fork/Join (FJ) framework. Some key differences between future tasks and regular tasks in the FJ framework are as follows:

- A future task extends the [`RecursiveTask`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/RecursiveTask.html) class in the FJ framework, instead of [`RecursiveAction`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/RecursiveAction.html) as in regular tasks.
- The `compute()` method of a future task must have a non-void return type, whereas it has a void return type for regular tasks.
- A method call like `left.join()` waits for the task referred to by object 𝑙𝑒𝑓𝑡left in both cases, but also provides the task’s return value  in the case of future task.

Code example of `RecursiveTask`:

```Java
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.ForkJoinPool;

public class SumTask extends RecursiveTask<Integer> {
    private static final int THRESHOLD = 10; // threshold for splitting tasks
    private int[] arr;
    private int start;
    private int end;

    public SumTask(int[] arr, int start, int end) {
        this.arr = arr;
        this.start = start;
        this.end = end;
    }

    @Override
    protected Integer compute() {
        if (end - start <= THRESHOLD) {
            // If the task is small enough, compute it directly
            int sum = 0;
            for (int i = start; i < end; i++) {
                sum += arr[i];
            }
            return sum;
        } else {
            // Split the task into smaller subtasks
            int middle = (start + end) / 2;
            SumTask leftTask = new SumTask(arr, start, middle);
            SumTask rightTask = new SumTask(arr, middle, end);

            // Fork the left task
            leftTask.fork();
            
            // Compute the right task directly
            int rightResult = rightTask.compute();

            // Join the left task result
            int leftResult = leftTask.join();

            // Combine the results
            return leftResult + rightResult;
        }
    }

    public static void main(String[] args) {
        // Create an array of integers to sum
        int[] arr = new int[100];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = i + 1;
        }

        // Create a ForkJoinPool to execute the task
        ForkJoinPool pool = new ForkJoinPool();
        System.out.println("Default parallelism: " + pool.getParallelism());
        SumTask task = new SumTask(arr, 0, arr.length);

        // Execute the task and get the result
        int result = pool.invoke(task);

        // Print the result
        System.out.println("Sum: " + result);
    }
}

```

#### Memorization

Memorization can be especially helpful for algorithms based on [dynamic programming](https://en.wikipedia.org/wiki/Dynamic_programming). The memorization pattern lends itself easily to parallelization using futures by modifying the memorized data structure to store

{(x1, y1 = future(f(x1))), (x2, y2 = future(f(x2))), . . .}. 

The lookup operation can then be replaced by a `get()` operation on the future value, if a future has already been created for the result of a given input.

#### Java Streams

Java Streams, introduced in Java 8, provide a modern, functional-style approach to processing sequences of elements (such as collections) in a declarative way. An aggregate data query or data transformation can be specified by  building a stream pipeline consisting of a *source* (typically by invoking the `.stream()` method on a data collection, a sequence of **intermediate** operations such as `map()` and `filter()`, and an optional **terminal** operation such as `forEach()` or `average()`. It is very easy to change stream to parallel stream.

```java
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StreamExample {
    public static void main(String[] args) {
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie", "David", "Edward");

        List<String> filteredNames = names.stream()
            .filter(name -> name.startsWith("A") || name.startsWith("E")) // Intermediate operation: filter
            .map(String::toUpperCase) // Intermediate operation: map
            .sorted() // Intermediate operation: sorted
            .collect(Collectors.toList()); // Terminal operation: collect

        filteredNames.forEach(System.out::println); // Terminal operation: forEach
    }
}

public class ParallelStreamExample {
    public static void main(String[] args) {
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie", "David", "Edward");

        List<String> filteredNames = names.parallelStream() // Use parallelStream() for parallel processing
            .filter(name -> name.startsWith("A") || name.startsWith("E")) // Intermediate operation: filter
            .map(String::toUpperCase) // Intermediate operation: map
            .sorted() // Intermediate operation: sorted
            .collect(Collectors.toList()); // Terminal operation: collect

        filteredNames.forEach(System.out::println); // Terminal operation: forEach
    }
}
```

C# has a similar concept called Parallel LINQ (PLINQ).

#### Determinism and Data Races

A parallel program is said to be

- functionally deterministic: if it always computes the same answer when given the same input.
- structurally deterministic: if it always computes the same computation graph, when given the same input.

An example of computation graph:

```text
Consider a simple example where you have a series of tasks with dependencies:

Task A: Read data from a file.
Task B: Process data (depends on A).
Task C: Perform computation 1 (depends on B).
Task D: Perform computation 2 (depends on B).
Task E: Write results to a file (depends on C and D).
The computation graph for this example would look like this:

   A
   |
   B
  / \
 C   D
  \ /
   E

Critical Path: The longest path through the graph, which determines the minimum time required to complete all tasks. The length of the critical path is a crucial factor in understanding the potential for parallel speedup.
```

The presence of data races often leads to functional and/or structural nondeterminism because a parallel program with data races may exhibit different behaviors for the same input, depending on the relative scheduling and timing of memory accesses involved in a data race.

Furthermore, there may be cases of **benign** nondeterminism for programs with data races in which different executions with the same input may generate different outputs, but all the outputs may be acceptable in the context of the application, e.g., different locations for a search pattern in a target string.