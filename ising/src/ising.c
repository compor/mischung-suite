#include <limits.h>
#include <math.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

#define HAS_UNISTD_H

#ifdef HAS_UNISTD_H
#include <unistd.h>
#endif /* HAS_UNISTD_H */

#define N 2010
#define ny 10
#define THETA 0.01
#define LIMIT -1000000000

#define MIN(a, b) (a > b) ? (b) : (a)

long nodes_num = N;
float theta = THETA;
int limit = LIMIT;
int debug_mode = 0;

struct node {
  uint32_t spins[ny];
  struct node *next;
};

int potential(struct node *node) {
  int sum = 0;
  int temp, i, j, kx, ky;
  int order = 31;
  for (i = 0; i < ny; i++) {
    for (j = 0; j < 32; j++) {
      temp = 0;
      /* To order kx and ky */
      for (kx = -order; kx < order + 1; kx++) {
        for (ky = -order; ky < order + 1; ky++) {
          if ((kx != 0) || (ky != 0)) {
            /*fprintf(stderr, "kx %d\n", kx);*/
            /*fprintf("stderr, ky %d\n", ky);*/
            /* BUG FIX with abs() since produced indices where also negative */
            temp += (1 - (((node->spins[abs((i + ny + ky)) % ny] >>
                            ((j + 32 + kx) % 32)) +
                           1) &
                          0x1) *
                             2);
          }
        }
      }
      /*Adding them to the total energy */
      sum -= temp * (1 - (((node->spins[i] >> j) + 1) & 0x1) * 2);
    }
  }

  return sum / 2;
}

void print_nodes(long n, struct node *node, int *energy);

void handle_args(int argc, char *argv[]) {
#ifdef HAS_UNISTD_H
  int opt;
  long nodes_num_tmp = 0;

  while ((opt = getopt(argc, argv, ":s:t:n:l:d")) != -1) {
    switch (opt) {
    case 's':
      srand(atol(optarg));
      break;
    case 't':
      theta = atof(optarg);
      break;
    case 'n':
      nodes_num_tmp = atoi(optarg);
      if (nodes_num_tmp > 0) {
        nodes_num = nodes_num_tmp;
      }
      break;
    case 'l':
      limit = atoi(optarg);
      break;
    case 'd':
      debug_mode = 1;
      break;
    case ':':
      fprintf(stderr, "option needs a value\n");
      exit(EXIT_FAILURE);
      break;
    case '?':
      fprintf(stderr, "unknown option : %c\n", optopt);
      exit(EXIT_FAILURE);
      break;
    }
  }
#else
  /* Override default random seed.  */
  if (argc > 1) {
    srand(atol(argv[1]));
  }

  /* Override default magnetization.  */
  if (argc > 2) {
    theta = atof(argv[2]);
  }

  if (argc > 3) {
    long nodes_num_tmp = atoi(argv[3]);
    if (nodes_num_tmp > 0) {
      nodes_num = nodes_num_tmp;
    }
  }

  if (argc > 4) {
    limit = atoi(argv[4]);
  }

  if (argc > 5) {
    debug_mode = atoi(argv[5]);
  }
#endif /* HAS_UNISTD_H */
}

int main(int argc, char *argv[]) {
  register struct node *node;
  struct node *nodes = NULL;
  int i, j;
  int *energy = NULL;
  int first_min_energy = INT_MAX;
  int first_min_energy_found = 0;
  int min_energy = INT_MAX;

  handle_args(argc, argv);

  nodes = (struct node *) malloc(nodes_num * sizeof(struct node));
  if (!nodes) {
    return -1;
  }

  energy = (int *) malloc(nodes_num * sizeof(int));
  if (!energy) {
    free(nodes);
    return -1;
  }

  /* Set up the initial linked list.  */
  for (i = 0; i < nodes_num - 1; i++) {
    for (j = 0; j < ny; j++)
      nodes[i].spins[j] = (rand() < (theta * RAND_MAX)) ? 1 : 0;
    nodes[i].next = &nodes[i + 1];
  }

  /* Terminate the linked list.  */
  for (j = 0; j < ny; j++)
    nodes[nodes_num - 1].spins[j] = 0;
  nodes[nodes_num - 1].next = 0;

  /* Search for a node with negative potential energy.  */
  i = 0;
  for (node = &nodes[0]; node; node = node->next) {
    energy[i] = potential(node);
    i++;
  }

  for (i = 0; i < nodes_num; i++) {
    if (!first_min_energy_found && energy[i] < limit) {
      first_min_energy = energy[i];
      first_min_energy_found = 1;
    }

    min_energy = MIN(min_energy, energy[i]);
  }

  if (debug_mode) {
    print_nodes(nodes_num, nodes, energy);
  }

  if (first_min_energy_found) {
    fprintf(stderr, "first min energy found: %d\n", first_min_energy);
  }
  fprintf(stderr, "min energy found: %d\n", min_energy);

  free(nodes);
  free(energy);

  return first_min_energy;
}

void print_nodes(long n, struct node *node, int *energy) {
  if (!node || !energy) {
    return;
  }

  for (unsigned long i = 0; i < n - 1; i++) {
    fprintf(stderr, "node %lu with energy %d and spins: ", i, energy[i]);
    for (unsigned long j = 0; j < ny - 1; j++) {
      fprintf(stderr, "%u ", node[i].spins[j]);
    }
    fprintf(stderr, "\n");
  }
}
