# [Coursera] Parallel, Concurrent, and Distributed Programming in Java Specialization

## Parallel Programming in Java

### Module 1: Course Introduction

### Module 2: Task Parallelism

Tasks are the most basic unit of parallel programming. An increasing number of programming languages (including Java and C++) are moving from older **thread-based** approaches to more modern **task-based** approaches for parallel programming.



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

- Functionally deterministic: if it always computes the same answer when given the same input.
- Structurally deterministic: if it always computes the same computation graph, when given the same input.

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

```
// sample: data race and structurally deterministic, but not functionally deterministic
c = 0;
forall (i : [0 : N]) {
  c = c + a[i];
}
println("c = " + c);
```

The presence of data races often leads to functional and/or structural nondeterminism because a parallel program with data races may exhibit different behaviors for the same input, depending on the relative scheduling and timing of memory accesses involved in a data race.

Furthermore, there may be cases of **benign** nondeterminism for programs with data races in which different executions with the same input may generate different outputs, but all the outputs may be acceptable in the context of the application, e.g., different locations for a search pattern in a target string.

### Module 4: Talking to Two Sigma: Using it in the Field

The managing director of Two Sigma:

> Well, the key thing we look for is a solid education. And in computer science, that means really understanding the fundamentals. So the basics like data structures, and algorithms, and operating systems, and computer systems, extremely important, low level programming understanding. What happens when that try-statement for exception handling comes up? What's it actually doing?...
>
> - Do all the homework and really program. 
> - Think about how the principles that you're learning can be applied to different kinds of things (NOT limited to CS area itself)
>
> ...We've seen an amazing uptake in interest from students in machine learning. And it goes hand in hand as you've said with parallel and distributed computing.

Software engineers of Two Sigma:

> Useful theoretical concepts:
>
> - Amdahl's Law: Emphasizes the importance of minimizing the **sequential portion** of a task to maximize the benefits of parallel computing.
> - Critical path length.
>
> Getting **as much coding experience outside the classroom as possible** was really helpful, whether that's in a formal internship or something more casual, just working on an independent project.

### Module 5: Loop Parallelism

#### Parallel Loops

The most general way is to think of each iteration of a parallel loop as an *async* task, with a *finish* construct encompassing all iterations. This approach can support general cases such as parallelization of the following pointer-chasing while loop (in pseudocode):

```
finish {
	for (p = head; p != null; p = p.next) 
		async compute(p);
}
```

However, further efficiencies can be gained by paying attention to *counted-for* loops for which the number of iterations is known on entry to the loop (before the loop executes its first iteration). We then learned the *forall* notation for expressing parallel counted-for loops:

```
forall (i : [0:n-1]) 
	a[i] = b[i] + c[i];
```

Java streams can be an elegant way of specifying parallel loop computations that produce a single output array, e.g., by rewriting the vector addition statement as follows:

```
a = IntStream.rangeClosed(0, N-1)
	.parallel()
	.toArray(i -> b[i] + c[i]);
```

In summary, streams are a convenient notation for parallel loops with at most one output array, but the *forall* notation is more convenient for loops that create/update **multiple output** arrays, as is the case in many scientific computations.

#### Parallel Matrix Multiplication

A simple sequential algorithm for two *n* *× n* matrices multiplication as follows:

```
for(i : [0:n-1]) {
  for(j : [0:n-1]) { 
  	c[i][j] = 0;
    for(k : [0:n-1]) {
      c[i][j] = c[i][j] + a[i][k]*b[k][j]
    }
  }
}
```

The interesting question now is: which of the for-i, for-j and for-k loops can be converted to *forall* loops, i.e., can be executed in parallel? Upon a close inspection, we can see that it is safe to **convert for-i and for-j into forall** loops, but for-k must remain a sequential loop to avoid data races. There are some trickier ways to also exploit parallelism in the for-k loop, but they rely on the observation that summation is algebraically associative even though it is computationally non-associative.

#### Barriers in Parallel Loops

The *barrier* construct through a simple example that began with the following *forall* parallel loop:

```
// HELLO and BYE's orders are random, e.g., BYE1 could run before HELLO2
forall (i : [0:n-1]) {
        myId = lookup(i); // convert int to a string 
        print HELLO, myId;
        print BYE, myId;
}

// What if we want to all BYE run after HELLO? -> Barrier
forall (i : [0:n-1]) {
        myId = lookup(i); // convert int to a string 
        print HELLO, myId; // Phase 0
        NEXT; // Barrier
        print BYE, myId; // Phase 1
}
```

Barriers are a fundamental construct for parallel loops that are used in a majority of real-world parallel applications including Cuda, OpenCL.

#### One-Dimensional Iterative Averaging

The [Jacobi method](https://en.wikipedia.org/wiki/Jacobi_method) for solving such equations typically utilizes two arrays, oldX[] and newX[]. A naive approach to parallelizing this method would result in the following pseudocode:

```
for (iter: [0:nsteps-1]) {
  forall (i: [1:n-1]) {
    newX[i] = (oldX[i-1] + oldX[i+1]) / 2;
  }
  swap pointers newX and oldX;
}
```

Though easy to understand, this approach creates *nsteps ×* (*n −* 1) tasks, which is too many. Barriers can help reduce the number of tasks created as follows:

```
forall ( i: [1:n-1]) {
  localNewX = newX; localOldX = oldX; // this make the newX stores the final result
  for (iter: [0:nsteps-1]) {
    localNewX[i] = (localOldX[i-1] + localOldX[i+1]) / 2;
    NEXT; // Barrier
    swap pointers localNewX and localOldX;
  }
}

// Or if we do not want localNewX and localOldX
forall ( i: [1:n-1]) {
  for (iter: [0:nsteps-1]) {
    newX[i] = (oldX[i-1] + oldX[i+1]) / 2;
    NEXT; // Barrier
    if (iter < nsteps - 1) {
      // Swap oldX and newX for the next iteration
      swap pointers newX and oldX;
    }
  }
}
```

In this case, only (*n −* 1) tasks are created, and there are *nsteps* * (*n −* 1) barrier operations. This is a significant improvement since creating **tasks is usually more expensive (overhead) than performing barrier operations**.

#### Iteration Grouping/Chunking in Parallel Loops

Chunking in computer science refers to a strategy for managing data or tasks by **breaking them into smaller**, more manageable pieces, known as "chunks."

The vector addition  example:

```
forall (i : [0:n-1]) a[i] = b[i] + c[i]
```

This approach creates *n* tasks, one per *forall* iteration, which is wasteful when (as is common in practice) *n* is much larger than the number of available processor cores. To address this problem, we learned a common tactic used in practice that is referred to as *loop* *chunking* or *iteration grouping*, and focuses on reducing the number of tasks created to be closer to the number of processor cores, so as to reduce the overhead of parallel execution:

```
forall (g:[0:ng-1])
  for (i : mygroup(g, ng, [0:n-1])) a[i] = b[i] + c[i]
```

This reduce the number of task to *ng*. There are two well known approaches for iteration grouping: *block* and *cyclic*. The *block* maps consecutive iterations to the same group, whereas the cyclic maps iterations in the same congruence class (mod *ng*) to the same group.
