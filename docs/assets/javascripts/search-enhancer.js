/**
 * UniGUI Documentation - Search Enhancer
 * Adds support for:
 *   - ClassName::methodName (e.g., WidgetBase::hasStyleClass, DrawScope::rect)
 *   - ClassName#methodName
 *   - ClassName.methodName
 *   - methodName() or methodName(args...)
 *   - bare methodName
 */
(function () {
  'use strict';

  function transformQuery(q) {
    if (!q || typeof q !== 'string') return q;
    var clean = q.trim();

    //    Qualified method pattern: ClassName::methodName, ClassName#methodName, ClassName.methodName
    //    Supports optional arguments like WidgetBase::id() or WidgetBase::rect(float, float)
    var qualified = clean.match(/^([A-Za-z0-9_$]+)(?:::|#|\.)([A-Za-z0-9_$]*)(?:\(.*?\))?$/);
    if (qualified) {
      var cls = qualified[1];
      var member = qualified[2];
      if (!member) {
        return '+' + cls;
      }
      return '+' + cls + ' +' + member + '*';
    }

    // Bare method with argument parentheses: methodName() or methodName(arg)
    var methodCall = clean.match(/^([A-Za-z0-9_$]+)\(.*?\)$/);
    if (methodCall) {
      return methodCall[1] + '*';
    }

    return q;
  }

  // Hook Web Worker so the search worker receives the enhanced query
  if (typeof window !== 'undefined' && window.Worker) {
    var OriginalWorker = window.Worker;
    window.Worker = function (scriptUrl, options) {
      var worker = new OriginalWorker(scriptUrl, options);
      try {
        var isSearchWorker = typeof scriptUrl === 'string' && scriptUrl.indexOf('search') !== -1;
        if (isSearchWorker) {
          var originalPostMessage = worker.postMessage.bind(worker);
          worker.postMessage = function (msg, transfer) {
            if (msg && msg.type === 2 && typeof msg.data === 'string') {
              msg.data = transformQuery(msg.data);
            }
            if (arguments.length > 1) {
              return originalPostMessage(msg, transfer);
            }
            return originalPostMessage(msg);
          };
        }
      } catch (err) {
        console.warn('[UniGUI] Search enhancer hook error:', err);
      }
      return worker;
    };
    window.Worker.prototype = OriginalWorker.prototype;
  }
})();
