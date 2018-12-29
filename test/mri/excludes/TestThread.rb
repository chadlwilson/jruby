# frozen_string_literal: false
exclude(/_stack_size$/, 'often too expensive')
exclude :test_abort_on_exception, "very sensitive to thread timing and parallelism"
exclude :test_inspect_with_fiber, "Thread.current is known to reflect the fiber, not the thread, in JRuby"
exclude :test_handle_interrupt, "handle_interrupt is not working properly #5314"
exclude :test_handle_interrupt_and_p, "handle_interrupt is not working properly #5314"
exclude :test_handle_interrupt_blocking, "handle_interrupt is not working properly #5314"
exclude :test_list, "JVM finalizer threads get adopted, randomly skewing the result (#4960)"
exclude :test_main_thread_status_at_exit, "we do not set main thread to aborting when it terminates; differing opinions on thread lifecycle at main shutdown"
exclude :test_main_thread_variable_in_enumerator, "differing notions of current thread within a fiber"
exclude :test_mutex_interrupt, "hangs"
exclude :test_mutex_unlock_on_trap, "traps execute in a separate thread in JRuby, and can't unlock a mutex"
exclude :test_no_valid_cfp, "may depend on MRI internal stack structures"
exclude :test_priority, "unreliably depends on thread scheduling"
exclude :test_recursive_outer, "expected to be fixed by our change based on MRI r43981 but was not"
exclude :test_report_on_exception, "very sensitive to thread timing and parallelism"
exclude :test_safe_level, "SAFE levels are unsupported"
exclude :test_signal_at_join, "very timing-sensitive and very edge casey"
exclude :test_thread_join_in_trap, "fails intermittently (#4956)"
exclude :test_thread_join_main_thread, "hangs"
exclude :test_thread_setname_in_initialize, "needs investigation #4308"
exclude :test_thread_status_raise_after_kill, "aborting status does not show up as expected"
exclude :test_thread_timer_and_interrupt, "event statuses do not match expected"
exclude :test_thread_value_in_trap, "fails intermittently (#4956)"
exclude :test_thread_variable_in_enumerator, "fibers in JRuby have their own Thread.current"