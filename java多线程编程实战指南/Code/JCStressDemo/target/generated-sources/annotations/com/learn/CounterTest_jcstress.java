package com.learn;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.openjdk.jcstress.infra.runners.TestConfig;
import org.openjdk.jcstress.infra.collectors.TestResultCollector;
import org.openjdk.jcstress.infra.runners.Runner;
import org.openjdk.jcstress.infra.runners.StateHolder;
import org.openjdk.jcstress.util.Counter;
import org.openjdk.jcstress.vm.WhiteBoxSupport;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Callable;
import java.util.Collections;
import java.util.List;
import com.learn.CounterTest;
import org.openjdk.jcstress.infra.results.L_Result_jcstress;
import com.learn.CounterTest_StateObject_jcstress;

public class CounterTest_jcstress extends Runner<L_Result_jcstress> {

    CounterTest test;
    volatile StateHolder<CounterTest_StateObject_jcstress, L_Result_jcstress> version;

    public CounterTest_jcstress(TestConfig config, TestResultCollector collector, ExecutorService pool) {
        super(config, collector, pool, "com.learn.CounterTest");
    }

    @Override
    public Counter<L_Result_jcstress> sanityCheck() throws Throwable {
        Counter<L_Result_jcstress> counter = new Counter<>();
        sanityCheck_API(counter);
        sanityCheck_Footprints(counter);
        return counter;
    }

    private void sanityCheck_API(Counter<L_Result_jcstress> counter) throws Throwable {
        final CounterTest_StateObject_jcstress s = new CounterTest_StateObject_jcstress();
        final L_Result_jcstress r = new L_Result_jcstress();
        final CounterTest t = new CounterTest();
        Collection<Future<?>> res = new ArrayList<>();
        res.add(pool.submit(() -> t.actor1(s)));
        res.add(pool.submit(() -> t.actor2(s)));
        for (Future<?> f : res) {
            try {
                f.get();
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        }
        try {
            pool.submit(() ->t.actor3(r, s)).get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }
        counter.record(r);
    }

    private void sanityCheck_Footprints(Counter<L_Result_jcstress> counter) throws Throwable {
        config.adjustStrides(size -> {
            version = new StateHolder<>(new CounterTest_StateObject_jcstress[size], new L_Result_jcstress[size], 2, config.spinLoopStyle);
            final CounterTest t = new CounterTest();
            for (int c = 0; c < size; c++) {
                L_Result_jcstress r = new L_Result_jcstress();
                CounterTest_StateObject_jcstress s = new CounterTest_StateObject_jcstress();
                version.rs[c] = r;
                version.ss[c] = s;
                t.actor1(s);
                t.actor2(s);
                t.actor3(r, s);
                counter.record(r);
            }
        });
    }

    @Override
    public Counter<L_Result_jcstress> internalRun() {
        test = new CounterTest();
        version = new StateHolder<>(new CounterTest_StateObject_jcstress[0], new L_Result_jcstress[0], 2, config.spinLoopStyle);

        control.isStopped = false;

        List<Callable<Counter<L_Result_jcstress>>> tasks = new ArrayList<>();
        tasks.add(this::actor1);
        tasks.add(this::actor2);
        Collections.shuffle(tasks);

        Collection<Future<Counter<L_Result_jcstress>>> results = new ArrayList<>();
        for (Callable<Counter<L_Result_jcstress>> task : tasks) {
            results.add(pool.submit(task));
        }

        try {
            TimeUnit.MILLISECONDS.sleep(config.time);
        } catch (InterruptedException e) {
        }

        control.isStopped = true;

        waitFor(results);

        Counter<L_Result_jcstress> counter = new Counter<>();
        for (Future<Counter<L_Result_jcstress>> f : results) {
            try {
                counter.merge(f.get());
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }
        return counter;
    }

    public final void jcstress_consume(StateHolder<CounterTest_StateObject_jcstress, L_Result_jcstress> holder, Counter<L_Result_jcstress> cnt, int a, int actors) {
        CounterTest_StateObject_jcstress[] ss = holder.ss;
        L_Result_jcstress[] rs = holder.rs;
        int len = ss.length;
        int left = a * len / actors;
        int right = (a + 1) * len / actors;
        for (int c = left; c < right; c++) {
            L_Result_jcstress r = rs[c];
            CounterTest_StateObject_jcstress s = ss[c];
            test.actor3(r, s);
            ss[c] = new CounterTest_StateObject_jcstress();
            cnt.record(r);
            r.r1 = null;
        }
    }

    public final void jcstress_updateHolder(StateHolder<CounterTest_StateObject_jcstress, L_Result_jcstress> holder) {
        if (!holder.tryStartUpdate()) return;
        CounterTest_StateObject_jcstress[] ss = holder.ss;
        L_Result_jcstress[] rs = holder.rs;
        int len = ss.length;

        int newLen = holder.updateStride ? Math.max(config.minStride, Math.min(len * 2, config.maxStride)) : len;

        CounterTest_StateObject_jcstress[] newS = ss;
        L_Result_jcstress[] newR = rs;
        if (newLen > len) {
            newS = Arrays.copyOf(ss, newLen);
            newR = Arrays.copyOf(rs, newLen);
            for (int c = len; c < newLen; c++) {
                newR[c] = new L_Result_jcstress();
                newS[c] = new CounterTest_StateObject_jcstress();
            }
         }

        version = new StateHolder<>(control.isStopped, newS, newR, 2, config.spinLoopStyle);
        holder.finishUpdate();
   }

    public final Counter<L_Result_jcstress> actor1() {

        CounterTest lt = test;
        Counter<L_Result_jcstress> counter = new Counter<>();
        while (true) {
            StateHolder<CounterTest_StateObject_jcstress,L_Result_jcstress> holder = version;
            if (holder.stopped) {
                return counter;
            }

            CounterTest_StateObject_jcstress[] ss = holder.ss;
            L_Result_jcstress[] rs = holder.rs;
            int size = ss.length;

            holder.preRun();

            for (int c = 0; c < size; c++) {
                CounterTest_StateObject_jcstress s = ss[c];
                s.trap = 0;
                lt.actor1(s);
            }

            holder.postRun();

            jcstress_consume(holder, counter, 0, 2);
            jcstress_updateHolder(holder);

            holder.postUpdate();
        }
    }

    public final Counter<L_Result_jcstress> actor2() {

        CounterTest lt = test;
        Counter<L_Result_jcstress> counter = new Counter<>();
        while (true) {
            StateHolder<CounterTest_StateObject_jcstress,L_Result_jcstress> holder = version;
            if (holder.stopped) {
                return counter;
            }

            CounterTest_StateObject_jcstress[] ss = holder.ss;
            L_Result_jcstress[] rs = holder.rs;
            int size = ss.length;

            holder.preRun();

            for (int c = 0; c < size; c++) {
                CounterTest_StateObject_jcstress s = ss[c];
                s.trap = 0;
                lt.actor2(s);
            }

            holder.postRun();

            jcstress_consume(holder, counter, 1, 2);
            jcstress_updateHolder(holder);

            holder.postUpdate();
        }
    }

}
