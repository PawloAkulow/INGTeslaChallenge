import random
import argparse
import json

parser = argparse.ArgumentParser(description='Generate file with ATM service tasks.')
parser.add_argument('--filename', type=str, required=True, help='Output file name')
parser.add_argument('--lines', type=int, required=True, help='Number of lines in the file')
args = parser.parse_args()

lines = args.lines
filename = args.filename

# percentage of tasks with priority, failure restart or low signal status
priority_pct = 0.1
failure_restart_pct = 0.1
signal_low_pct = 0.1

# create a list of task types based on the percentages
task_types = []
task_types.extend(['STANDARD'] * int(lines * (1 - priority_pct - failure_restart_pct - signal_low_pct)))
task_types.extend(['PRIORITY'] * int(lines * priority_pct))
task_types.extend(['FAILURE_RESTART'] * int(lines * failure_restart_pct))
task_types.extend(['SIGNAL_LOW'] * int(lines * signal_low_pct))

# shuffle the task types list
random.shuffle(task_types)

# create the tasks list
tasks = []
for i in range(lines):
	region = i % 9999 + 1  # use modulus operator to keep region within 1-9999 range
	atm_id = random.randint(1, 9999)  # ensure unique atm_id within region
	tasks.append({
    		"region": region,
    		"requestType": task_types[i],
    		"atmId": atm_id
	})

# write the tasks list to the file
with open(filename, 'w') as f:
    for task in tasks:
        f.write(f'{json.dumps(task)},\n')
