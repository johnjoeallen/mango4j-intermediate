#!/usr/bin/env python3
"""Generates the exception logging scenarios report from the output of ExceptionLoggingScenariosTest.

Run the test first (it writes each scenario's captured log output to target/scenario-output/):

    mvn -o test -Dtest=ExceptionLoggingScenariosTest     (in mango4j-logging)
    python3 tools/generate_exception_logging_scenarios.py   (in mango4j-spring-logging)

The report is written to EXCEPTION_LOGGING_SCENARIOS.md in the module and to the docs page
mango4j-spring/docs/mango4j-spring/mango4j-spring-logging/exception-logging-scenarios.md,
or to the paths given as arguments. Each scenario's Setup is taken from the test fixtures.
"""
import json, os, sys, xml.etree.ElementTree as ET
from pathlib import Path
HERE = Path(__file__).resolve().parent
MOD = str(HERE.parent)                       # mango4j-logging/mango4j-spring-logging
REPO = HERE.parents[2]                       # repository root
OUTS = sys.argv[1:] or [
    f'{MOD}/EXCEPTION_LOGGING_SCENARIOS.md',
    str(REPO / 'mango4j-spring/docs/mango4j-spring/mango4j-spring-logging/exception-logging-scenarios.md'),
]
D = {
 'singleMethod_default_logsMessageCauseAndStackTrace': ('', 'One ERROR entry, message `boom`. `reason` is `boom`; `stacktrace` is an object (`exception`, `message`, `frames`) with the cause nested inside it as `stacktrace.cause`, which has the same shape. There is no top-level `cause`.'),
 'singleMethod_emptyDetail_logsNothing': ('Method with `@LoggableMethod(logException = {})` throws.', 'Nothing logged. The exception still propagates to the caller.'),
 'singleMethod_classNameOnly_noMessageNoStackTrace': ('`@LoggableMethod(logException = {REASON, CLASS_NAME})`, message is `boom-secret`.', 'Reason is the class name only; message text and stack trace are absent.'),
 'singleMethod_messageOnly_noCauseNoStackTrace': ('', 'Reason is `boom`; the cause and the stack trace are absent.'),
 'singleMethod_stackTraceOnly_stackTraceWithoutReason': ('', '`stacktrace` has the exception class and its `frames` but no `message` (MESSAGE is not selected); no top-level `reason`; empty log message.'),
 'singleMethod_stackTraceOnly_neverLeaksMessagesFromTheExceptionOrItsCauses': ('', '`stacktrace` has the exception class and frames only: no `message` and no nested `cause` (CAUSE is not selected). Neither message appears anywhere.'),
 'singleMethod_messageAndStackTrace_messageShownInTraceNotAsReason': ('', 'No top-level `reason`. `stacktrace.message` is `boom-MSG`, and there is no nested cause: CAUSE is not selected.'),
 'singleMethod_messageCauseAndStackTrace_causeNodeHasMessageAndOwnTrace': ('', 'No top-level `reason` (REASON is not selected). `stacktrace` has `exception`, `message` and `frames`, with the cause nested as `stacktrace.cause` (its own `exception`, `message` and `frames`).'),
 'singleMethod_causeAndStackTrace_causeNodeHasClassNamesOnly': ('', 'The top `stacktrace` and the nested `cause` both have `exception` and `frames` but no `message`; no message appears anywhere.'),
 'singleMethod_classNameAndStackTrace_traceHasNoMessages': ('', '`reason` is the class name; `stacktrace` has the class and frames, with no `message` and no nested cause (CAUSE is not selected).'),
 'cause_reasonAndCause_causeIsNestedInTheStackTraceWithItsOwnReason': ('', '`reason` is `boom`. The cause is nested inside the stack trace (`stacktrace.cause`) with its `exception`, `message` (`inner`) and `frames`; there is no top-level `cause`.'),
 'cause_withoutStackTrace_doesNothing': ('', 'CAUSE only adds to STACK_TRACE: without it nothing about the cause is logged. `reason` is just `boom`; there is no `stacktrace` and no `cause`, and `inner` appears nowhere.'),
 'cause_chainOfThreeExceptions_nestedOneInsideTheOther': ('', "`reason` is outer; `stacktrace.cause.message` is middle; `stacktrace.cause.cause.message` is inner. Each cause is nested inside its parent's trace."),
 'cause_causeWithoutMessage_reasonIsClassName': ('', 'The nested cause has `exception` `java.lang.IllegalArgumentException` and no `message`.'),
 'cause_selectedButExceptionHasNoCause_nothingAppended': ('', '`reason` is just `boom`; `stacktrace` has no `cause`.'),
 'cause_defaultDetail_causeNodeHasItsOwnStackTrace': ('', 'The top `stacktrace` has no `Caused by` entries; `stacktrace.cause` has its own `exception`, `message` (`inner`) and `frames`.'),
 'cause_deepChainWithStackTraceOnly_eachCauseIsNestedInItsParentsTrace': ('', 'Each level is a `cause` object inside its parent, with `exception` and `frames` but no `message`; none of the messages appear.'),
 'cause_depthLimit_onlyFiveNestedLevels': ('', 'The causes nest five deep (`level-1` … `level-5`); `level-6` and `level-7` are not logged.'),
 'loggableClassChain_default_exceptionNodeIsOnlyForTheThrownExceptionAndIsMasked': ('', '`exception` (the fields object) is only for the thrown `PaymentDeclinedException` (`class` + fields, card masked as `4111********1111`). The causes, nested in `stacktrace.cause`, have `exception` (just the class name), `message` and `frames` but no `@LoggableField` values. The raw card number, the account number and `failedAttempts` appear nowhere.'),
 'loggableClassException_exceptionAndCauseWithoutStackTrace_noCauseIsLogged': ('', '`reason` and the masked `exception` node only. CAUSE has no effect without STACK_TRACE, so no cause (and no stack trace) is logged.'),
 'singleMethod_messageAndCause_causeIsNestedInTheStackTrace': ('', '`reason` is `boom` (no `; Cause:` suffix); the cause is `stacktrace.cause` with `message` `inner` and its own frames.'),
 'precedence_onExceptionOverridesLogException': ('`@OnException(type = IOException.class, detail = {REASON, CLASS_NAME})` on a method with `@LoggableMethod(logException = {REASON, MESSAGE})`; throws `IOException("disk-secret")`.', 'Class name only; the message text is absent.'),
 'precedence_onExceptionNotMatching_fallsBackToLogException': ('Same method, but throws `IllegalStateException` (no rule matches).', 'Falls back to `@LoggableMethod(logException = {REASON, MESSAGE})`: message `boom`.'),
 'precedence_globalRuleOverridesLogException': ('Global rule `.on(IllegalStateException.class, REASON, CLASS_NAME)`; the method is a plain `@LoggableMethod` (default detail).', 'Class name only; message and stack trace absent.'),
 'precedence_globalRuleWithNoDetail_logsNothing': ('Global rule `.on(IllegalStateException.class)` (no detail); the method is a plain `@LoggableMethod`.', 'Nothing logged.'),
 'precedence_globalRuleNotMatching_fallsBackToLogException': ('Global rule `.on(IOException.class, REASON, CLASS_NAME)` only; the method throws `IllegalStateException` and has `@LoggableMethod(logException = {REASON, MESSAGE})`.', 'Falls back to the method: message `boom`.'),
 'precedence_onExceptionOverridesGlobalRule': ('Global rule `.on(IOException.class)` (no detail), but the method has `@OnException(type = IOException.class, detail = {REASON, CLASS_NAME})`.', '`@OnException` wins: class name is logged.'),
 'onException_emptyDetail_logsNothingForTheMatchingType': ('', "Nothing logged: the rule's empty detail applies to `IllegalStateException`."),
 'onException_typeArray_ruleAppliesToEveryListedType': ('', 'Class name only (`java.lang.IllegalStateException`); `boom-MSG` and the stack trace are absent. The `type` array lists two types and either one matches.'),
 'onException_defaultType_matchesAnyException': ('', '`type` defaults to `Throwable`, so the rule matches `IOException`: `reason` is `disk full`, overriding `logException = {}`.'),
 'onException_repeated_firstMatchingRuleWins_broadRuleFirst': ('', 'Rules are checked in declaration order: the broad `RuntimeException` rule comes first, so only the class name is logged.'),
 'onException_repeated_firstMatchingRuleWins_specificRuleFirst': ('', 'The specific `IllegalStateException` rule comes first, so the message `boom-MSG` is logged.'),
 'onException_ruleForASuperclass_matchesSubclasses': ('', '`PaymentException` is a subclass of `RuntimeException`, so the rule matches: message `card declined`.'),
 'onException_fullDetail_reasonCauseNodeAndStackTraces': ('', '`reason`, a `stacktrace` with the `message` and `frames`, and the cause nested inside it with its own `message` and `frames`.'),
 'onException_exceptionDetail_logsMaskedLoggableClassFields': ('', '`reason` and an `exception` node with the masked card `4111********1111`; no stack trace.'),
 'onException_differentRulesPerType_ioExceptionGetsClassNameOnly': ('', 'The `IOException` rule applies: class name only.'),
 'onException_differentRulesPerType_paymentExceptionGetsMessageAndStackTrace': ('', 'The `PaymentException` rule applies: `reason` and a `stacktrace` with the message.'),
 'onException_overridesAnEmptyLogException_forTheMatchingType': ('', '`@OnException` overrides `logException = {}` for `IllegalStateException`: the message `boom` is logged.'),
 'onException_nonMatchingTypeFallsBackToAnEmptyLogException_nothingLogged': ('', "No rule matches `IOException`, so the method's `logException = {}` applies: nothing logged."),
 'onException_classNameAndMessage_classNameThenMessage': ('', '`reason` is `java.lang.IllegalStateException: boom`.'),
 'onException_parentOptsInWithEmptyDetail_onlyTheChildShowsTheException': ('', 'The parent opts in (its rule matches) but its detail is empty, so it logs nothing for the exception; only the child shows it (one `reason`).'),
 'onException_parentOptsInWithDifferentDetail_eachItemUsesItsOwnDetail': ('', "Each item uses its own detail: the parent's `reason` is the class name, the child's is the message."),
 'onException_parentRuleForAnotherType_doesNotOptIn_childExceptionStaysOnTheChild': ('', "The parent's rule is for `IOException`, so it does not opt in: the exception appears only under the child."),
 'parentPropagatesChildException_loggedOnceAtChild': ('Parent (no `@OnException`) calls a child that throws; parent lets it propagate.', 'One entry; the exception appears once, under the child (`called`). The parent has no `reason`.'),
 'parentWithMatchingOnException_exceptionNodeOnBothItemsButMessageOnlyOnce': ('Same, but the parent has `@OnException(type = PaymentException.class, detail = {REASON, MESSAGE})`.', 'The exception is recorded on the child and on the parent (two `reason`s), but the log message holds it once: `card declined`.'),
 'parentWrapsChildException_wrapperLoggedByParentAndChildKeepsOriginal': ('Parent catches the child exception and throws `RuntimeException("order failed", cause)`.', 'Wrapper is a different instance, so the parent logs it as well as the child (two `reason`s).'),
 'parentThrowsUnrelatedExceptionWithoutCause_childDetailsStillLogged': ('Parent catches the child exception and throws a NEW `IllegalStateException("order failed")` with **no cause**, so the child exception is not nested in it. Child has `@LoggableMethod(logException = {REASON, MESSAGE, CAUSE})`.', 'Child exception is still logged under the child (`called`), and the parent logs its own exception. Two `reason`s; message `card declined; order failed` (child first).'),
 'childWithEmptyDetailParentThrowsUnrelatedException_onlyParentExceptionLogged': ('Same, but the child has `@LoggableMethod(logException = {})`.', 'Only the parent\'s new exception is logged; the child exception (`card-secret`) appears nowhere.'),
 'exceptionPropagatesThroughTwoParents_loggedOnce': ('Grandparent -> parent -> child; the child throws and both parents let it propagate.', 'Logged once, at the child.'),
 'childWithEmptyDetailPropagatesThroughPlainParent_nothingLoggedAtAll': ('Child has `@LoggableMethod(logException = {})`; parent (default detail) lets it propagate.', 'Nothing logged at all. The child logs nothing by choice and the parent skips the same instance.'),
 'childWithEmptyDetailWrappedByParent_wrapperIsLogged': ('Child has `@LoggableMethod(logException = {})`; parent wraps it in a new exception.', 'The parent logs the wrapper (message `outer failed`).'),
 'globalRuleDoesNotMakeParentRerecordChildException': ('A global rule matches the exception; parent has no `@OnException`.', 'Still logged once: only `@OnException` makes a parent re-record.'),
 'loggableClassException_default_reasonTextAndExceptionFieldsLoggedSeparately': ('Exception class is `@LoggableClass` with a `@LoggableField`; plain `@LoggableMethod` (default detail).', 'Two separate keys: `reason` is the text (`secret failure`) and `exception` is an object with a `class` field (the exception class name) plus the structured fields (`code: X-42`).'),
 'loggableClassException_exceptionOnly_fieldsWithoutReasonText': ('Same exception, `@LoggableMethod(logException = {EXCEPTION})`.', 'Only the `exception` key (with `X-42`); no `reason` text and no stack trace.'),
 'loggableClassException_reasonOnly_textWithoutFields': ('Same exception, `@LoggableMethod(logException = {REASON, MESSAGE})`.', 'Only the `reason` text; no `exception` key and `X-42` appears nowhere.'),
 'loggableClassException_classNameOnly_fieldsAreNotLogged': ('Same exception, `@LoggableMethod(logException = {REASON, CLASS_NAME})`.', '`reason` is the class name; no `exception` key; `X-42` appears nowhere.'),
}

import re as _re
TEST_SRC = open(f'{MOD}/src/test/java/ie/bitstep/mango/spring/logging/internal/ExceptionLoggingScenariosTest.java', encoding='utf-8').read()

def _balanced(text, start):
    """index of the matching close paren for the open paren at text[start]"""
    depth, i, instr = 0, start, False
    while i < len(text):
        c = text[i]
        if instr:
            if c == '\\': i += 1
            elif c == '"': instr = False
        elif c == '"': instr = True
        elif c == '(': depth += 1
        elif c == ')':
            depth -= 1
            if depth == 0: return i
        i += 1
    raise ValueError('unbalanced')

def _split_top(text):
    parts, depth, cur, instr = [], 0, '', False
    i = 0
    while i < len(text):
        c = text[i]
        if instr:
            cur += c
            if c == '\\': cur += text[i+1]; i += 1
            elif c == '"': instr = False
        elif c == '"': instr = True; cur += c
        elif c in '([{': depth += 1; cur += c
        elif c in ')]}': depth -= 1; cur += c
        elif c == ',' and depth == 0: parts.append(cur.strip()); cur = ''
        else: cur += c
        i += 1
    if cur.strip(): parts.append(cur.strip())
    return parts

def _method_body(name):
    m = _re.search(r'void ' + _re.escape(name) + r'\(', TEST_SRC)
    i = TEST_SRC.index('{', m.end()); depth = 0; instr = False; j = i
    while j < len(TEST_SRC):
        c = TEST_SRC[j]
        if instr:
            if c == '\\': j += 1
            elif c == '"': instr = False
        elif c == '"': instr = True
        elif c == '{': depth += 1
        elif c == '}':
            depth -= 1
            if depth == 0: return TEST_SRC[i+1:j]
        j += 1
    raise ValueError(name)

FIXTURES = {}
for m in _re.finditer(r'public static class (\w+) \{\n((?:\t\t@[^\n]*\n)+)\t\tpublic void call\(\)', TEST_SRC):
    FIXTURES[m.group(1)] = [l.strip() for l in m.group(2).strip().splitlines()]

THROWN_OVERRIDE = {
 'cause_depthLimit_onlyFiveNestedLevels': 'new RuntimeException("level-0", new RuntimeException("level-1", /* levels 2-6 */ new RuntimeException("level-7")))',
}
LOGGABLE_CHAIN = ('new PaymentDeclinedException("Payment declined for order 1042", "1042", "4111111111111111", "05",\n'
                  '        new AccountLockedException("Account locked after 5 failed attempts", "ACC-77889900", 5,\n'
                  '            new IOException("Connection reset")))')
ROLE_NAMES = {1: ['call'], 2: ['parent', 'child'], 3: ['grandparent', 'parent', 'child']}

def setup_snippet(test_name):
    body = _method_body(test_name)
    out = []
    m = _re.search(r'ExceptionDetailResolver\.setStrategy\(ExceptionDetailRules\.builder\(\)(.*?)\.build\(\)\)', body, _re.S)
    if m:
        out.append('// ExceptionDetailStrategy bean (global rules)')
        out.append('ExceptionDetailRules.builder()')
        for on in _re.findall(r'\.on\(([^)]*)\)', m.group(1)):
            out.append(f'    .on({on.strip()})')
        out.append('    .build();')
        out.append('')
    i = body.index('run(')
    j = _balanced(body, i + 3)
    args_text = _re.sub(r'//[^\n]*', '', body[i+4:j])
    args = _split_top(args_text)
    thrown, hops = _re.sub(r'\s+', ' ', args[0]), args[1:]
    if test_name in THROWN_OVERRIDE: thrown = THROWN_OVERRIDE[test_name]
    elif 'declinedBecauseLocked' in thrown: thrown = LOGGABLE_CHAIN
    elif thrown == 'chain': thrown = THROWN_OVERRIDE.get(test_name, 'chain')
    parsed = []
    for h in hops:
        h = _re.sub(r'\s+', ' ', h)
        fx = _re.search(r'new (\w+)\(\)', h).group(1)
        wrap = None
        if h.startswith('Hop.wrapping('):
            parts = _split_top(h[len('Hop.wrapping('):-1])
            wrap = parts[1].split('->', 1)[1].strip()
            wrap = _re.sub(r'\bt\b', 'e', wrap)
        parsed.append((fx, wrap))
    names = ROLE_NAMES[len(parsed)]
    methods = []
    for idx, (fx, wrap) in enumerate(parsed):
        ann = FIXTURES[fx]
        lines = list(ann) + [f'void {names[idx]}() {{']
        if idx == len(parsed) - 1:
            lines.append(f'    throw {thrown};')
        elif wrap:
            lines += ['    try {', f'        {names[idx+1]}();', '    } catch (RuntimeException e) {', f'        throw {wrap};', '    }']
        else:
            lines.append(f'    {names[idx+1]}();')
        lines.append('}')
        methods.append('\n'.join(lines))
    out.append('\n\n'.join(methods))
    return '\n'.join(out)

def abbreviate_trace(t):
    """stacktrace object: keep exception/message/cause; keep only the first frame of each exception."""
    out = {}
    for k, v in t.items():
        if k == 'frames' and isinstance(v, list):
            out[k] = v[:1] + ([{'…': f'{len(v)-1} more frames'}] if len(v) > 1 else [])
        elif k == 'cause' and isinstance(v, dict):
            out[k] = abbreviate_trace(v)
        else:
            out[k] = v
    return out
def abbreviate(o):
    if isinstance(o, dict):
        r = {}
        for k, v in o.items():
            if k == 'stacktrace' and isinstance(v, dict):
                r[k] = abbreviate_trace(v)
            else:
                r[k] = abbreviate(v)
        return r
    if isinstance(o, list):
        return [abbreviate(x) for x in o]
    return o
def render(path):
    txt = open(path, encoding='utf-8').read()
    if txt.startswith('(nothing logged)'):
        return '(nothing logged)'
    out = []
    for block in txt.strip().split('\nLEVEL:')[0:]:
        pass
    entries, cur = [], None
    for line in txt.splitlines():
        if line.startswith('LEVEL:'):
            cur = {'level': line[8:].strip()}; entries.append(cur)
        elif line.startswith('MESSAGE:'):
            cur['message'] = line[9:].strip()
        elif line.startswith('LOG:'):
            cur['log'] = json.loads(line[9:].strip())
    res = []
    for e in entries:
        res.append(f"level:   {e['level']}\nmessage: {e['message']!r}\nlog:\n" + json.dumps(abbreviate(e['log']), indent=2, ensure_ascii=False))
    return '\n\n'.join(res)
results = {}
for tc in ET.parse(f'{MOD}/target/surefire-reports/TEST-ie.bitstep.mango.spring.logging.internal.ExceptionLoggingScenariosTest.xml').getroot().findall('testcase'):
    results[tc.get('name')] = 'pass' if tc.find('failure') is None and tc.find('error') is None else 'FAIL'
others = {}
for cls in ['dc.DiagnosticContextExceptionDedupeTest','internal.AspectExceptionDedupeTest','internal.ExceptionDetailResolverTest','detail.ExceptionDetailStrategyTest','internal.DiagnosticContextLoggerExceptionDetailTest','internal.DiagnosticContextItemExceptionDetailTest']:
    r = ET.parse(f'{MOD}/target/surefire-reports/TEST-ie.bitstep.mango.spring.logging.{cls}.xml').getroot()
    others[cls.split('.')[-1]] = (int(r.get('tests')), int(r.get('failures')) + int(r.get('errors')))
L = ['# Exception logging — end-to-end scenarios\n',
 '*Generated from the tests, do not edit by hand. To regenerate, run `mvn -o test -Dtest=ExceptionLoggingScenariosTest` in `mango4j-logging`, then `python3 tools/generate_exception_logging_scenarios.py` in `mango4j-spring-logging`.*\n',
 'Generated from `ExceptionLoggingScenariosTest` (`mango4j-spring-logging`), which writes each scenario\'s captured output to `target/scenario-output/`. Each scenario runs the real aspect delegate, the real `DiagnosticContextLogger` and the real level strategy; '
 'a capturing adapter records what would be logged. Each **Setup** is the annotated method(s) the scenario runs, taken from the test fixtures (a parent `void parent()` calls `child()`; a global-rules scenario shows the `ExceptionDetailRules` bean). Timestamps and response times are fixed. `stacktrace` is an object (`exception`, `message`, `frames`, and with CAUSE a nested `cause` of the same shape); frames are shortened to the first one per exception here.\n',
 f'**{len(results)} scenarios — {sum(v=="pass" for v in results.values())} passed, {sum(v!="pass" for v in results.values())} failed.**\n',
 'Two behaviours worth knowing, both visible below:\n',
 '- A child with `@LoggableMethod(logException = {})` that propagates through a plain parent leaves **no log entry at all** (the child logs nothing by choice; the parent skips the same instance).',
 '- When a parent opts in with `@OnException`, the exception is recorded on both items (two `reason`s) but the joined log *message* lists it once.\n']
for i, (name, (does, exp)) in enumerate(D.items(), 1):
    p = f'{MOD}/target/scenario-output/{name}.txt'
    L.append(f'\n## {i}. `{name}` — {"✅" if results.get(name)=="pass" else "❌"}\n')
    L.append('**Setup:**\n\n```java\n' + setup_snippet(name) + '\n```\n')
    L.append(f'**Expected:** {exp}\n')
    L.append('**Log output:**\n\n```\n' + render(p) + '\n```')
L.append('\n---\n\nOther unit tests for the same functionality (assert on mocks or contexts, no log output): ' +
         ', '.join(f'`{k}` ({n} tests, {"all pass" if f==0 else str(f)+" failing"})' for k, (n, f) in others.items()) + '.')
for _out in OUTS:
    open(_out, 'w', encoding='utf-8').write('\n'.join(L) + '\n')
missing = [k for k in results if k not in D]
print(len(results), 'scenarios;', 'undocumented:', missing)
