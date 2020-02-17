#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

#define N 2010
#define ny 10

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
            // printf("%d", kx);
            // printf("%d\n", ky);
            temp +=
                (1 -
                 (((node->spins[(i + ny + ky) % ny] >> ((j + 32 + kx) % 32)) +
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

int main(int argc, char *argv[]) {
  register struct node *node;
  struct node nodes[N];
  float theta = 0.01;
  int i, energy, j;

  /* Override default random seed.  */
  if (argc > 1)
    srandom(atol(argv[1]));

  /* Override default magnetization.  */
  if (argc > 2)
    theta = atof(argv[2]);

  /* Set up the initial linked list.  */
  for (i = 0; i < N - 1; i++) {
    for (j = 0; j < ny; j++)
      nodes[i].spins[j] = (random() < (theta * RAND_MAX)) ? 1 : 0;
    nodes[i].next = &nodes[i + 1];
  }

  /* Terminate the linked list.  */
  for (j = 0; j < ny; j++)
    nodes[N - 1].spins[j] = 0;
  nodes[N - 1].next = 0;

  /* Search for a node with negative potential energy.  */
  for (node = &nodes[0]; node; node = node->next) {
    energy = potential(node);
#if 0
        printf("%d\n", energy);
        fflush(stdout);
#endif
    if (energy < -1000000000)
      break;
  }

  return energy;
}
