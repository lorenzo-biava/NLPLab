import multiprocessing
import logging
import time
import math


def do_tasks_in_chunk(from_index, to_index, task_list, task_func, kwargs):
    i = from_index
    results = list()
    while i <= to_index:
        r = task_func(i, task_list[i], kwargs)
        results.append(r)
        i += 1
    return results


class ParallelTasks:

    def __init__(self, label=None, progress_interval=5, processes=0):
        self._result_holder = dict()
        self._logger = None
        self._progress_interval = 5
        self._last_progress = 0
        self._started = 0
        self._ended = 0
        self._processes = 0

        log_name = 'ParallelTasks'
        if label is not None:
            log_name += '-' + label
        self._logger = logging.getLogger(log_name)
        self._progress_interval = progress_interval
        self._processes = processes

    def log_level(self, level):
        self._logger.setLevel(level)

    def log_result(self, result):
        # This is called whenever worker function returns a result.
        # _result_holder is modified only by the main process, not the pool workers.
        #print(multiprocessing.current_process())
        for entry in result:
            self._result_holder['result_dict'][entry['index']] = entry['value']
            self._result_holder['completed_tasks'] += 1

        if time.time() - self._last_progress > self._progress_interval:
            self._last_progress = time.time()
            self._logger.info(
                "Progress: %d/%d (%.0f%%)" % (
                    self._result_holder['completed_tasks'], self._result_holder['total_tasks'], (
                        self._result_holder['completed_tasks'] / self._result_holder['total_tasks'] * 100)))

    @staticmethod
    def rebuild_list(result):
        r = list()
        for i in range(len(result)):
            r.append(result.get(i))
        return r

    def apply_async_with_callback(self, task_list, task_func, processes=0, chunks=0, **kwargs):
        self._logger.debug("Instantiating pool...")
        if processes != 0:
            pool = multiprocessing.Pool(processes)
        elif self._processes != 0:
            pool = multiprocessing.Pool(self._processes)
        else:
            pool = multiprocessing.Pool()

        tasks_count = len(task_list)

        if chunks == 0:
            chunks = math.floor(tasks_count/min(tasks_count/10, 30))
            #chunks = pool._processes


        step = math.floor(tasks_count / chunks)

        self._logger.debug("Pool ready with %d processes, %d chuncks" % (pool._processes, chunks))
        self._started = time.time()
        i = 0
        self._result_holder = {'result_dict': dict(), 'total_tasks': len(task_list), 'completed_tasks': 0}
        results = list()
        # for entry in task_list:
        # results.append(pool.apply_async(task_func, args=(i, entry, kwargs, ), callback=self.log_result))
        # i += 1
        c = 0
        for i in range(0, tasks_count - 1, step):
            # call the *worker* function with start and stop values.
            c = c + 1
            to = min(i + step, tasks_count - 1, )
            self._logger.debug("Chunck %d, from %d to %d" % (c, i, to))

            results.append(pool.apply_async(func=do_tasks_in_chunk, args=(
                i, to, task_list, task_func, kwargs,),
                                            callback=self.log_result))

        pool.close()

        # pool = multiprocessing.Pool(processes=4)
        # i = 0
        # results=list()
        # for entry in task_list:
        #    results.append(pool.apply_async(task_func, args=(i, entry,)))
        #    i += 1


        # JOIN NOT WORKING !
        #pool.join()
        output = [p.get() for p in results]
        pool.terminate()

        result_list = self.rebuild_list(self._result_holder['result_dict'])
        self._stopped = time.time()
        self._logger.debug("Work complete ! (%f s)" % (self._stopped - self._started))
        return result_list