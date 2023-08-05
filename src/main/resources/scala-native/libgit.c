#include <string.h>
#include "git2.h"

void __sn_wrap_libgit_div(int _0, int _1, div_t *____return) {
  div_t ____ret = div(_0, _1);
  memcpy(____return, &____ret, sizeof(div_t));
}


void __sn_wrap_libgit_imaxdiv(intmax_t __numer, intmax_t __denom, imaxdiv_t *____return) {
  imaxdiv_t ____ret = imaxdiv(__numer, __denom);
  memcpy(____return, &____ret, sizeof(imaxdiv_t));
}


void __sn_wrap_libgit_ldiv(long _0, long _1, ldiv_t *____return) {
  ldiv_t ____ret = ldiv(_0, _1);
  memcpy(____return, &____ret, sizeof(ldiv_t));
}


void __sn_wrap_libgit_lldiv(long long _0, long long _1, lldiv_t *____return) {
  lldiv_t ____ret = lldiv(_0, _1);
  memcpy(____return, &____ret, sizeof(lldiv_t));
}


