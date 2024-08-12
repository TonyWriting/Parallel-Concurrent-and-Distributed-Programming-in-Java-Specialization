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

```java
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

```java
// sample: data race and structurally deterministic, but not functionally deterministic
c = 0;
forall (i: [0 : N]) {
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

```java
a = IntStream.rangeClosed(0, N-1)
	.parallel()
	.toArray(i -> b[i] + c[i]);
```

In summary, streams are a convenient notation for parallel loops with at most one output array, but the *forall* notation is more convenient for loops that create/update **multiple output** arrays, as is the case in many scientific computations.

#### Parallel Matrix Multiplication

A simple sequential algorithm for two *n* *× n* matrices multiplication as follows:

```java
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

```java
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

```java
for (iter : [0:nsteps-1]) {
  forall (i : [1:n-1]) {
    newX[i] = (oldX[i-1] + oldX[i+1]) / 2;
  }
  swap pointers newX and oldX;
}
```

Though easy to understand, this approach creates *nsteps ×* (*n −* 1) tasks, which is too many. Barriers can help reduce the number of tasks created as follows:

```java
forall (i : [1:n-1]) {
  localNewX = newX; localOldX = oldX; // this make the newX stores the final result
  for (iter: [0 : nsteps-1]) {
    localNewX[i] = (localOldX[i-1] + localOldX[i+1]) / 2;
    NEXT; // Barrier
    swap pointers localNewX and localOldX;
  }
}

// Or if we do not want localNewX and localOldX
forall (i : [1:n-1]) {
  for (iter : [0:nsteps-1]) {
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
forall (i : [0:n-1]) 
  a[i] = b[i] + c[i]
```

This approach creates *n* tasks, one per *forall* iteration, which is wasteful when (as is common in practice) *n* is much larger than the number of available processor cores. To address this problem, we learned a common tactic used in practice that is referred to as *loop* *chunking* or *iteration grouping*, and focuses on reducing the number of tasks created to be closer to the number of processor cores, so as to reduce the overhead of parallel execution:

```
forall (g : [0:ng-1])
  for (i : mygroup(g, ng, [0:n-1]))
    a[i] = b[i] + c[i]
```

This reduce the number of task to *ng*. There are two well known approaches for iteration grouping: *block* and *cyclic*. The *block* maps consecutive iterations to the same group, whereas the cyclic maps iterations in the same congruence class (mod *ng*) to the same group.

### Module 6: Data Flow Synchronization and Pipelining

#### Split-phase Barriers with Java  Phasers

Assuming the process time of lookup and barrier is 100 unit, and ignoring the print time, the critical path length (CPL) of the following code is 100 + 100 = 200:

```java
// its critical path length is 200
forall (i : [0:n-1]) { 
  print HELLO, i;
  myId = lookup(i);
  NEXT; // Barrier, equals to phaser.arriveAndAwaitAdvance(), arrives and waits for others to arrive.
  print BYE, myId;
}
```

However, upon closer examination, we can see that the call to lookup(i) is local to iteration i and that there is no specific need to either complete it before the barrier or to complete it after the barrier. In fact, the call to lookup(i) can be performed in parallel with the barrier. To facilitate this *split-phase barrier* (also known as a *fuzzy barrier*) we use two separate APIs from Java Phaser class — ph.arrive() and ph.awaitAdvance(). Together these two APIs form a barrier, but we now have the freedom to insert a computation such as lookup(i) between the two calls as follows:

```java
// its critical path length is 100
// initialize phaser ph	for use by n tasks ("parties")
// n tasks (or threads) must call ph.arrive() (or any related method like arriveAndAwaitAdvance()) for the phaser to advance to the next phase.
Phaser ph = new Phaser(n);
// Create forall loop with n iterations that operate on ph 
forall (i : [0:n-1]) {
  print HELLO, i;
  // Arrives at the current phase but doesn't wait for others.
  // phase represents the current phase number before the arrival
  // If the phaser was just created and no phases have been completed yet, the current phase is 0.
  int phase = ph.arrive();
  myId = lookup(i);
  ph.awaitAdvance(phase);
  print BYE, myId;
}
```

#### Point-to-Point Synchronization with Phasers

Suppose we have following 3 tasks, what is the critical path length if simply putting a full barrier?

|      | Task0             | Task1                | Task2             |
| ---- | ----------------- | -------------------- | ----------------- |
| 1    | X = A(), cost = 1 | Y = B(), cost = 2    | Z = C(), cost = 3 |
| 2    | D(X, Y), cost = 3 | E(X, Y, Z), cost = 2 | F(Y, Z), cost = 1 |

Apparently, the critical path length is Cost(C()) + Cost(D(X, Y)) = 3 + 3 = 6. However, D(X, Y) actually has not dependency on C(), so they should be able to run parallelly. To increase the parallelism, we can ensure that we are only waiting for the dependencies according to the functional use of the variables, or point-to-point synchronization:

![image-20240721160540367](./Img/point2point-sync.png)

With phasers, the critical path length is reduced from 6 to 5.

#### One-Dimensional Iterative Averaging with Phasers

A full barrier is not necessary since *forall* iteration *i* only needs to wait for iterations *i* *−* 1 and *i* + 1 to complete their current phase before iteration *i* can move to its next phase. This idea can be captured by phasers, if we allocate an array of phasers as follows:

```java
// Allocate array of phasers
Phaser[] ph = new Phaser[n+2]; //array of phasers
for (int i = 0; i < ph.length; i++) ph[i] = new Phaser(1);

// Main computation 
forall (i: [1:n-1]) {
  for (iter: [0:nsteps-1]) {
    newX[i] = (oldX[i-1] + oldX[i+1]) / 2;
    // Indicates that the thread (or task) has completed its work for the current phase
    // This method increments the internal count of arrived parties
    ph[i].arrive();
    // iter represents the current phase (or stage) of the phaser
    if (index > 1) ph[i-1].awaitAdvance(iter);
    if (index < n-1) ph[i + 1].awaitAdvance(iter); 
    swap pointers newX and oldX;
  }
}
```

In this case, the average takes the same amount of time on barrier, but in some cases (sparse matrix calculations) this method ensures that you only wait for neighbors calculations give you more parallelism than barrier.

As we learned earlier, grouping/chunking of parallel iterations in a *forall* can be an important consideration for performance (due to reduced overhead). The idea of grouping of parallel iterations can be extended to *forall* loops with phasers as follows:

```java
// Allocate array of phasers proportional to number of chunked tasks 
Phaser[] ph = new Phaser[tasks+2]; //array of phasers
for (int i = 0; i < ph.length; i++) ph[i] = new Phaser(1);

// Main computation 
forall (i : [0:tasks-1]) {
  for (iter : [0:nsteps-1]) {
    // Compute leftmost boundary element for group
    int left = i * (n / tasks) + 1;
    myNew[left] = (myVal[left - 1] + myVal[left + 1]) / 2.0;
    
    // Compute rightmost boundary element for group 
    int right = (i + 1) * (n / tasks);
    myNew[right] = (myVal[right - 1] + myVal[right + 1]) / 2.0;
    
    // Signal arrival on phaser ph AND LEFT AND RIGHT ELEMENTS ARE AV 
    int	index = i + 1;
    ph[index].arrive();
    
    // Compute interior elements in parallel with barrier 
    for (int j = left + 1; j <= right - 1; j++)
      myNew[j] = (myVal[j - 1] + myVal[j + 1]) / 2.0;
    // Wait for previous phase to complete before advancing 
    if (index > 1) ph[index - 1].awaitAdvance(iter);
    if (index < tasks) ph[index + 1].awaitAdvance(iter);
    swap pointers newX and oldX;
  }
}
```

#### [Pipeline](https://en.wikipedia.org/wiki/Pipeline_(computing)) Parallelism

Let *n* be the number of input items and *p* the number of stages in the pipeline, *WORK* = *n* *×* *p* is the total work that must be done for all data items, and *CPL* = *n* + *p* *−*1 is the *SPAN* or critical path length for the pipeline. Thus, the ideal parallelism is *PAR* = *WORK* / *CPL* = *np* / (*n* + *p* *−* 1). When *n* is much larger than *p* (*n* » *p*), then the ideal parallelism approaches *PAR* = *p* in the limit, which is the best possible case.



![image-20240728113714279](./Img/pipeline.png)

The synchronization required for pipeline parallelism can be implemented using phasers by allocating an array of phasers, such that phaser ph[i] is “signaled” in iteration i by a call to ph[i].arrive() as follows:

```java
// Code for pipeline stage i
while ( there is an input to be processed) {
  // wait for previous stage, if any 
  if (i > 0) ph[i - 1].awaitAdvance(); 

  process input;

  // signal next stage
  ph[i].arrive();
}
```

#### Data Flow Parallelism

The simple data flow graph studied in the lecture consisted of five nodes and four edges: *A* *→ C, A* *→ D, B* *→ D, B* *→ E*. While futures can be used to generate such a computation graph, e.g., by including calls to A.get() and B.get() in task D, the computation graph edges are implicit in the get() calls when using futures. Instead, we introduced  the asyncAwait notation to specify a task along with an explicit set of preconditions (events that the task must wait for before it can start execution). With this approach, the program can be generated directly from the computation graph as  follows:



![image-20240728115548072](./Img/data-flow.png)

Interestingly, the order of the above statements is not significant. Just as a graph can be defined by enumerating its edges in any order, the above data flow program can be rewritten as follows, without changing its meaning:

```java
asyncAwait(A, () -> {/* Task C */} ); // Only execute task after event A is triggered 
asyncAwait(A, B, () -> {/* Task D */} ); // Only execute task after events A, B are triggered 
asyncAwait(B, () -> {/* Task E */} ); // Only execute task after event B is triggered 
async( () -> {/* Task A */; A.put(); } ); // Complete task and trigger event A
async( () -> {/* Task B */; B.put(); } ); // Complete task and trigger event B
```

Finally, we observed that the power and elegance of data flow parallel programming is accompanied by the possibility of a lack of progress that can be viewed as a form of **deadlock** if the program omits a put() call for signaling an event.

### Module 7: Speaking with industry professionals at Two Sigma

The software engineer of Two Sigma:

> The way I started out with concurrency is we used to learn all these low level constructs: threads, locks, semaphores and mutexes and you had to worry about the problems of deadlocks or data races. Lot of bugs.
>
> But eventually, we got introduced to higher level constructs, like deadlock-free locks, message passing techniques like actor model that avoid data races which make concurrent programming a lot easier. These are very important in industrial.

The senior vice president Two Sigma:

> We are looking for vey strong computer science foundational skills, including parallel and distributed computing. Not only multi-threading, but also frameworks like Apache Spark, OpenMP, MPI or any of these things that we can put to use very quickly and experienced with GPU. Communication skills, teamwork are also important besides fundamental skills because we are working on a very large system today. Technology is changing so quickly, and I think something that helped me a lot was that we focused very heavily on fundamental computer science skills, which are much more lasting with changes in the technology.

## Concurrent Programming in Java

### Module 2: Threads and Locks

#### Threads

A unique aspect of Java compared to prior mainstream programming languages is that Java included the notions of threads (as instances of the `java.lang.Threadjava.lang.Thread` class) in its language definition right from the start.

When an instance of Thread is *created* (via a new operation), it does not start executing right away; instead, it can only start executing when its start() method is invoked. The statement or computation to be executed by the thread is specified as a parameter to the constructor.

The Thread class also includes a *wait* operation in the form of a join()join() method. If thread t0t0 performs a t1.join() call, thread t0 will be forced to wait until thread t1 completes, after which point it can safely access any values computed by thread t1. Since there is no restriction on which thread can perform a join on which other thread, it is possible for a programmer to erroneously create a *deadlock cycle* with join operations. (A deadlock occurs when two threads wait for each other indefinitely, so that neither can make any progress.)

TODO: comparison with task in C#

#### Structured Locks

 Structured locks can be used to enforce *mutual exclusion* and avoid *data races*, as illustrated by the incr() method in the A.count example, and the insert() and remove() methods in the the Buffer example. A major benefit of structured locks is that their *acquire* and *release* operations are implicit, since these operations are automatically performed by the Java runtime environment when entering and exiting the scope of a synchronized statement or method, even if an exception is thrown in the middle.

We also learned about wait() and notify() operations that can be used to block and resume threads that need to wait for specific conditions. For example, a producer thread performing an insert() operation on a *bounded buffer* can call wait() when the buffer is full, so that it is only unblocked when a consumer thread performing a remove() operation calls notify(). Likewise, a consumer thread performing a remove() operation on a *bounded buffer* can call wait() when the buffer is empty, so that it is only unblocked when a producer thread performing an insert() operation calls notify(). Structured locks are also referred to as *intrinsic locks* or *monitors*.

The `synchronized` keyword is the traditional and simpler mechanism for synchronization in Java. When you use `synchronized`, the intrinsic lock (also known as the monitor) associated with an object or class is used automatically. The use of `synchronized` methods or statements provides access to the implicit monitor lock associated with every object, but forces all lock acquisition and release to occur in a block-structured way: when multiple locks are acquired they must be released in the opposite order, and all locks must be released in the same lexical scope in which they were acquired.

```Java
public class Counter {
    private int count = 0;

    public synchronized void increment() {
        count++;
    }

    public synchronized int getCount() {
        return count;
    }
}
```

The `synchronized` keyword in Java is conceptually similar to the `lock` statement in C#.

#### Unstructured Locks

In this lecture, we introduced *unstructured locks* (which can be obtained in Java by creating instances of  `ReentrantLock()`, and used three examples to demonstrate their generality relative to structured locks. The first example showed how explicit lock() and unlock() operations on unstructured locks can be used to support a *hand-over-hand* locking pattern that implements a non-nested pairing of lock/unlock operations which cannot be achieved with synchronized statements/methods. The second example showed how the `tryLock()` operations in unstructured locks can enable a thread to check the availability of a lock, and thereby acquire it if it is available or do something else if it is not. The third example illustrated the value of *read-write locks* (which can be obtained in Java by creating instances of `ReentrantReadWriteLock()`, whereby multiple threads are permitted to acquire a lock L in “read mode”, `L.readLock().lock()`, but only one thread is permitted to acquire the lock in “write mode”, `L.writeLock().lock()`.

However, it is also important to remember that the generality and power of unstructured locks is accompanied by an extra responsibility on the part of the programmer, e.g., ensuring that calls to `unlock()` are not forgotten, even in the presence of **exceptions**.

![image-20240810101651671](./Img/unstructured-locks.png)

The `Lock` interface, especially its implementation `ReentrantLock`, provides a more flexible and powerful mechanism for synchronization.  You must explicitly call the `lock()` method to acquire the lock and `unlock()` to release it, typically using a `try-finally` block to ensure the lock is released even if an exception occurs.

```java
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Counter {
    private int count = 0;
    private final Lock lock = new ReentrantLock();

    public void increment() {
        lock.lock();
        try {
            count++;
        } finally {
            lock.unlock();
        }
    }

    public int getCount() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }
}
```

So we learned about two mechanisms for performing synchronization in concurrent Java programs: Java synchronized statement and Java `ReentrantLock` class.

#### Liveness and Progress Guarantees

In this lecture, we studied three ways in which a parallel program may enter a state in which it stops making forward progress. For sequential programs, an “infinite loop” is a common way for a program to stop making forward progress, but there are other ways to obtain an absence of progress in a parallel program. 

The first is *deadlock*, in which all threads are blocked indefinitely, thereby preventing any forward progress. The second is *livelock*, in which all threads repeatedly perform an interaction that prevents forward progress, e.g., an infinite “loop” of repeating lock acquire/release patterns. The third is *starvation*, in which at least one thread is prevented from making any forward progress. 

The term “liveness” refers to a progress guarantee. The three progress guarantees that correspond to the absence of the conditions listed above are *deadlock freedom*, *livelock freedom*, and *starvation freedom*. 

![image-20240810103242618](./Img/liveness.png)

### Dining Philosophers

In this lecture, we studied a classical concurrent programming example that is referred to as the *Dining Philosophers Problem*. In this problem, there are five threads, each of which models a “philosopher” that repeatedly performs a sequence of actions which include *think, pick up chopsticks, eat*, and *put down chopsticks*. 

First, we examined a solution to this problem using structured locks, and demonstrated how this solution could lead to a deadlock scenario (but not livelock). Second, we examined a solution using unstructured locks with `tryLock()` and `unlock()` operations that never block, and demonstrated how this solution could lead to a livelock scenario (but not deadlock). Finally, we observed how a simple modification to the first solution with structured locks, in which one philosopher picks up their right chopstick and their left, while the others pick up their left chopstick first and then their right, can guarantee an absence of deadlock. However, this may still have starvation which could be solved by semaphore.

![image-20240810104949349](./Img/dining-philosophers.png)



