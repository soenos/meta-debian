#
# base recipe: meta/recipes-devtools/gcc/gcc-runtime_8.2.bb
# base branch: master
# base commit: da24071e92071ecbefe51314d82bf40f85172485
#

require gcc-8.inc
require recipes-devtools/gcc/gcc-runtime.inc

# Disable ifuncs for libatomic on arm conflicts -march/-mcpu
EXTRA_OECONF_append_arm = " libat_cv_have_ifunc=no "

FILES_libgomp-dev += "\
    ${libdir}/gcc/${TARGET_SYS}/${BINV}/include/openacc.h \
"

PTEST_PATH = "${libdir}/${PN}/ptest"
inherit autotools ptest

RPROVIDES_${PN}-ptest = "${PN}"
FILES_${PN}-ptest = "${PTEST_PATH}/*"
FILESEXTRAPATHS_prepend := "${THISDIR}/${PN}:"
RDEPENDS_${PN}-ptest += "coreutils findutils"

do_compile_ptest() {
    testsuite=`find ${B} -type d -name testsuite -print`
    for dir in $testsuite
    do
        if [ ! -f $dir/Makefile ]; then
            continue
        fi
        (
          cd $dir
          oe_runmake -i clean check
          targets=`echo test-*`
          oe_runmake -i clean
          if [ "$targets" != "test-*" ]; then
              (
                cd ..
                find . -type f \( -name '*.o' -o -name '*.a' \) -delete
                oe_runmake -i -e all
              )
              oe_runmake -i -e $targets
              ${THISDIR}/${PN}/create-test-runner
          fi
        )
    done
}

do_install_ptest() {
    install -d ${D}${PTEST_PATH}/tests/
    cp ${THISDIR}/${PN}/run-ptest ${D}${PTEST_PATH}/
    testsuite=`find ${B} -type d -name testsuite -print`
    for dir in $testsuite
    do
        tests=`find $dir -executable -type f -name 'test-*' -print`
        if [ -z "$tests" ]; then
            continue
        fi
        path=`dirname $dir`
        name=`basename $path`
        dst=${D}${PTEST_PATH}/tests/$name
        install -d $dst/
        cp $tests $dst/
        cp -r $dir/src $dst/
    done
}
