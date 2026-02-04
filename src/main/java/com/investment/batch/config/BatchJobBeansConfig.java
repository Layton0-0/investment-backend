package com.investment.batch.config;

import com.investment.batch.tasklet.*;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch Job/Step 빈 정의.
 * Job 빈 이름 = Job 이름 = 레지스트리 id.
 */
@Configuration
public class BatchJobBeansConfig {

        private static Step step(JobRepository jobRepository, PlatformTransactionManager tx,
                        String stepName, Tasklet tasklet) {
                return new StepBuilder(stepName, jobRepository)
                                .tasklet(tasklet, tx)
                                .build();
        }

        private static Job job(JobRepository jobRepository, String jobName, Step step) {
                return new JobBuilder(jobName, jobRepository)
                                .start(step)
                                .build();
        }

        @Bean(name = "trading-portfolio-generator")
        public Job tradingPortfolioGeneratorJob(JobRepository jobRepository,
                        PlatformTransactionManager tx,
                        TradingPortfolioTasklet tasklet) {
                return job(jobRepository, "trading-portfolio-generator",
                                step(jobRepository, tx, "trading-portfolio-step", tasklet));
        }

        @Bean(name = "short-term-strategy-executor")
        public Job shortTermStrategyJob(JobRepository jobRepository,
                        PlatformTransactionManager tx,
                        ShortTermStrategyTasklet tasklet) {
                return job(jobRepository, "short-term-strategy-executor",
                                step(jobRepository, tx, "short-term-step", tasklet));
        }

        @Bean(name = "medium-term-strategy-executor")
        public Job mediumTermStrategyJob(JobRepository jobRepository,
                        PlatformTransactionManager tx,
                        MediumTermStrategyTasklet tasklet) {
                return job(jobRepository, "medium-term-strategy-executor",
                                step(jobRepository, tx, "medium-term-step", tasklet));
        }

        @Bean(name = "long-term-strategy-executor")
        public Job longTermStrategyJob(JobRepository jobRepository,
                        PlatformTransactionManager tx,
                        LongTermStrategyTasklet tasklet) {
                return job(jobRepository, "long-term-strategy-executor",
                                step(jobRepository, tx, "long-term-step", tasklet));
        }

        @Bean(name = "dart-disclosure-collector")
        public Job dartCollectJob(JobRepository jobRepository,
                        PlatformTransactionManager tx,
                        DartCollectTasklet tasklet) {
                return job(jobRepository, "dart-disclosure-collector",
                                step(jobRepository, tx, "dart-step", tasklet));
        }

        @Bean(name = "sec-disclosure-collector")
        public Job secCollectJob(JobRepository jobRepository,
                        PlatformTransactionManager tx,
                        SecCollectTasklet tasklet) {
                return job(jobRepository, "sec-disclosure-collector",
                                step(jobRepository, tx, "sec-step", tasklet));
        }

        @Bean(name = "krx-daily-collector")
        public Job krxDailyCollectJob(JobRepository jobRepository,
                        PlatformTransactionManager tx,
                        KrxDailyCollectTasklet tasklet) {
                return job(jobRepository, "krx-daily-collector",
                                step(jobRepository, tx, "krx-daily-step", tasklet));
        }

        @Bean(name = "us-daily-collector")
        public Job usDailyCollectJob(JobRepository jobRepository,
                        PlatformTransactionManager tx,
                        UsDailyCollectTasklet tasklet) {
                return job(jobRepository, "us-daily-collector",
                                step(jobRepository, tx, "us-daily-step", tasklet));
        }

        @Bean(name = "factor-calculation")
        public Job factorCalculationJob(JobRepository jobRepository,
                        PlatformTransactionManager tx,
                        FactorCalculationTasklet tasklet) {
                return job(jobRepository, "factor-calculation",
                                step(jobRepository, tx, "factor-calculation-step", tasklet));
        }

        @Bean(name = "pipeline-execution")
        public Job pipelineExecutionJob(JobRepository jobRepository,
                        PlatformTransactionManager tx,
                        PipelineExecutionTasklet tasklet) {
                return job(jobRepository, "pipeline-execution",
                                step(jobRepository, tx, "pipeline-execution-step", tasklet));
        }

        @Bean(name = "pipeline-exit")
        public Job pipelineExitJob(JobRepository jobRepository,
                        PlatformTransactionManager tx,
                        PipelineExitTasklet tasklet) {
                return job(jobRepository, "pipeline-exit",
                                step(jobRepository, tx, "pipeline-exit-step", tasklet));
        }

        @Bean(name = "fill-confirmation")
        public Job fillConfirmationJob(JobRepository jobRepository,
                        PlatformTransactionManager tx,
                        FillConfirmationTasklet tasklet) {
                return job(jobRepository, "fill-confirmation",
                                step(jobRepository, tx, "fill-confirmation-step", tasklet));
        }

        @Bean(name = "unfilled-order-check")
        public Job unfilledOrderCheckJob(JobRepository jobRepository,
                        PlatformTransactionManager tx,
                        UnfilledOrderCheckTasklet tasklet) {
                return job(jobRepository, "unfilled-order-check",
                                step(jobRepository, tx, "unfilled-order-check-step", tasklet));
        }

        @Bean(name = "medium-term-rebalance")
        public Job mediumTermRebalanceJob(JobRepository jobRepository,
                        PlatformTransactionManager tx,
                        MediumTermRebalanceTasklet tasklet) {
                return job(jobRepository, "medium-term-rebalance",
                                step(jobRepository, tx, "medium-term-rebalance-step", tasklet));
        }

        @Bean(name = "daily-pnl")
        public Job dailyPnlJob(JobRepository jobRepository,
                        PlatformTransactionManager tx,
                        DailyPnlTasklet tasklet) {
                return job(jobRepository, "daily-pnl",
                                step(jobRepository, tx, "daily-pnl-step", tasklet));
        }

        @Bean(name = "intraday-breakout")
        public Job intradayBreakoutJob(JobRepository jobRepository,
                        PlatformTransactionManager tx,
                        IntradayBreakoutTasklet tasklet) {
                return job(jobRepository, "intraday-breakout",
                                step(jobRepository, tx, "intraday-breakout-step", tasklet));
        }

        @Bean(name = "robo-rebalance")
        public Job roboRebalanceJob(JobRepository jobRepository,
                        PlatformTransactionManager tx,
                        RoboRebalanceTasklet tasklet) {
                return job(jobRepository, "robo-rebalance",
                                step(jobRepository, tx, "robo-rebalance-step", tasklet));
        }
}
