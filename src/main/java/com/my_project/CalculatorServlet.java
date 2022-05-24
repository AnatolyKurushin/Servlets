package com.my_project;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.logging.Logger;


@WebServlet(urlPatterns = {"/calc/*"})
public class CalculatorServlet extends HttpServlet {
    public static final String SET_EXPRESSION = "expression";
    public static final String RESULT = "result";
    public static final String WHITESPACE = " ";
    public static final String CLOSING_PARENTHESIS = ")";
    public static final String OPENING_PARENTHESIS = "(";
    public static final String ALL_SYMBOL_AND_PARENTHESIS = "()+-/*";
    public static final String ALL_SYMBOLS = "+-/*";
    public static final String MULTIPLICATION_AND_DIVISION = "/*";
    public static final String ADDITION = "+";
    public static final String SUBTRACTION = "-";
    public static final String MULTIPLICATION = "*";
    public static final String DIVISION = "/";
    public static final char CHAR_ADDITION = '+';
    public static final char CHAR_SUBTRACTION = '-';
    public static final char CHAR_MULTIPLICATION = '*';
    public static final char CHAR_DIVISION = '/';
    public static final int MIN_VALUES_RANGE = -10000;
    public static final int MAX_VALUES_RANGE = 10000;
    public static final int SC_CREATED = 201;
    public static final int SC_NO_CONTENT = 204;
    public static final int SC_BAD_REQUEST = 400;
    public static final int SC_FORBIDDEN = 403;
    public static final int SC_CONFLICT = 409;
    private static final long serialVersionUID = 1;
    private static final Logger LOGGER = Logger.getLogger(CalculatorServlet.class.getName());

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        String keyInfo = new StringBuilder(request.getPathInfo()).substring(1);

        if (session.getAttribute(keyInfo) == null) {
            response.setStatus(SC_CREATED);
        }

        if (keyInfo.equals(SET_EXPRESSION)) {
            checkingExpression(request, response, keyInfo);
        } else {
            checkingValueSize(request, response, keyInfo);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        String variableForDelete = new StringBuilder(request.getPathInfo()).substring(1);
        session.setAttribute(variableForDelete, null);
        response.setStatus(SC_NO_CONTENT);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        try {
            List<String> tokens = parseIntoPolishNotation(session.getAttribute(SET_EXPRESSION).toString());
            Map<String, Integer> args = new HashMap<>();
            Map<String, String> variablesHavingNameOfAnotherVariable = new HashMap<>();

            Enumeration<String> allNamesAttributes = session.getAttributeNames();
            while (allNamesAttributes.hasMoreElements()) {
                String attributeName = allNamesAttributes.nextElement();
                if (attributeName.length() != 1) {
                    continue;
                }
                putAttributesInArgs(session, args, variablesHavingNameOfAnotherVariable, attributeName);
            }

            for (Map.Entry<String, String> keyVariable : variablesHavingNameOfAnotherVariable.entrySet()) {
                Integer valueVariable = args.get(variablesHavingNameOfAnotherVariable.get(keyVariable.getKey()));
                args.put(keyVariable.getKey(), valueVariable);
            }

            int result = calculate(tokens, args);
            session.setAttribute(RESULT, result);
        } catch (NumberFormatException exception) {
            LOGGER.info(exception.getMessage());
            response.setStatus(SC_CONFLICT);
        }

        try {
            response.getWriter().print(session.getAttribute(RESULT));
        } catch (IOException exception) {
            LOGGER.info(exception.getMessage());
        }
    }

    private void putAttributesInArgs(HttpSession session, Map<String, Integer> args,
                                     Map<String, String> variablesHavingNameOfAnotherVariable, String attributeName) {
        String valueOfAttribute = session.getAttribute(attributeName).toString();
        try {
            args.put(attributeName, Integer.parseInt(valueOfAttribute));
        } catch (NumberFormatException e) {
            variablesHavingNameOfAnotherVariable.put(attributeName, valueOfAttribute);
        }
    }


    private synchronized List<String> parseIntoPolishNotation(String equation) {
        List<String> tokens = new ArrayList<>();
        Deque<String> stackSymbols = new ArrayDeque<>();
        List<String> resultPolishNotation = new ArrayList<>();

        StringTokenizer tokenizer = new StringTokenizer(deleteWhitespaceInEquation(equation),
                ALL_SYMBOL_AND_PARENTHESIS, true);
        while (tokenizer.hasMoreElements()) {
            tokens.add(tokenizer.nextToken());
        }

        addAllTokensInResult(tokens, stackSymbols, resultPolishNotation);

        addRemainingTokens(stackSymbols, resultPolishNotation);
        return resultPolishNotation;
    }

    private String deleteWhitespaceInEquation(String equation) {
        StringBuilder result = new StringBuilder(equation);
        int lengthEquation = result.length();
        for (int i = 0; i < lengthEquation; i++) {
            int n = result.indexOf(WHITESPACE);
            if (n != -1) {
                result.deleteCharAt(n);
            } else {
                break;
            }
        }
        return result.toString();
    }

    private void addAllSymbols(Deque<String> stackSymbols, List<String> resultPolishNotation, String token) {
        while (ALL_SYMBOLS.contains(stackSymbols.getFirst())) {
            resultPolishNotation.add(stackSymbols.removeFirst());
        }
        if (token.equals(CLOSING_PARENTHESIS)) {
            stackSymbols.removeFirst();
        } else {
            stackSymbols.addFirst(token);
        }
    }

    private void addAdditionAndDivisionSymbols(Deque<String> stackSymbols, List<String> resultPolishNotation,
                                               String token) {
        while (MULTIPLICATION_AND_DIVISION.contains(stackSymbols.getFirst())) {
            resultPolishNotation.add(stackSymbols.removeFirst());
        }
        stackSymbols.addFirst(token);
    }

    private void addAllTokensInResult(List<String> tokens, Deque<String> stackSymbols,
                                      List<String> resultPolishNotation) {
        for (String token : tokens) {
            try {
                switch (token) {
                    case ADDITION:
                    case SUBTRACTION:
                    case CLOSING_PARENTHESIS:
                        addAllSymbols(stackSymbols, resultPolishNotation, token);
                        break;
                    case MULTIPLICATION:
                    case DIVISION:
                        addAdditionAndDivisionSymbols(stackSymbols, resultPolishNotation, token);
                        break;
                    case OPENING_PARENTHESIS:
                        stackSymbols.addFirst(token);
                        break;
                    default:
                        resultPolishNotation.add(token);
                }
            } catch (NoSuchElementException exception) {
                LOGGER.info(exception.getMessage());
                if (!token.equals(CLOSING_PARENTHESIS)) {
                    stackSymbols.addFirst(token);
                }
            }
        }
    }

    private void addRemainingTokens(Deque<String> stackSymbols, List<String> resultPolishNotation) {
        while (!stackSymbols.isEmpty()) {
            if (stackSymbols.getFirst().equals(OPENING_PARENTHESIS)) {
                return;
            }
            resultPolishNotation.add(stackSymbols.removeFirst());
        }
    }


    private synchronized int calculate(List<String> polishNotation, Map<String, Integer> args) {
        Deque<Integer> stackOfNumbers = new ArrayDeque<>();
        for (String token : polishNotation) {
            switch (token) {
                case ADDITION:
                    stackOfNumbers.addFirst(stackOfNumbers.removeFirst() + stackOfNumbers.removeFirst());
                    break;
                case SUBTRACTION:
                    subtractingNumbers(stackOfNumbers);
                    break;
                case MULTIPLICATION:
                    stackOfNumbers.addFirst(stackOfNumbers.removeFirst() * stackOfNumbers.removeFirst());
                    break;
                case DIVISION:
                    dividingNumbers(stackOfNumbers);
                    break;
                default:
                    addTokenInStack(args, stackOfNumbers, token);
            }
        }
        return stackOfNumbers.removeFirst();
    }

    private void subtractingNumbers(Deque<Integer> stackOfNumbers) {
        int secondNumber;
        int firstNumber;
        firstNumber = stackOfNumbers.removeFirst();
        secondNumber = stackOfNumbers.removeFirst();
        stackOfNumbers.addFirst(secondNumber - firstNumber);
    }

    private void dividingNumbers(Deque<Integer> stackOfNumbers) {
        int secondNumber;
        int firstNumber;
        firstNumber = stackOfNumbers.removeFirst();
        secondNumber = stackOfNumbers.removeFirst();
        stackOfNumbers.addFirst(secondNumber / firstNumber);
    }

    private void addTokenInStack(Map<String, Integer> args, Deque<Integer> stackOfNumbers, String token) {
        if (args.containsKey(token)) {
            stackOfNumbers.addFirst(args.get(token));
        } else {
            int arg = Integer.parseInt(token);
            stackOfNumbers.addFirst(arg);
        }
    }


    private void checkingExpression(HttpServletRequest request, HttpServletResponse response, String keyInfo) {

        String valueInfo = "";
        try {
            valueInfo = request.getReader().readLine();
        } catch (IOException exception) {
            LOGGER.info(exception.getMessage());
        }

        int count = 0;
        for (char ch : valueInfo.toCharArray()) {
            if (!(ch == CHAR_ADDITION || ch == CHAR_SUBTRACTION
                    || ch == CHAR_MULTIPLICATION || ch == CHAR_DIVISION)) {
                ++count;
            }
            if (count == valueInfo.length()) {
                response.setStatus(SC_BAD_REQUEST);
                return;
            }
        }
        HttpSession session = request.getSession();
        session.setAttribute(keyInfo, valueInfo);
    }

    private void checkingValueSize(HttpServletRequest request, HttpServletResponse response, String keyInfo) {
        HttpSession session = request.getSession();
        String valueInfo = "";
        try {
            valueInfo = request.getReader().readLine();

            int arg = Integer.parseInt(valueInfo);
            if (arg < MIN_VALUES_RANGE || arg > MAX_VALUES_RANGE) {
                response.setStatus(SC_FORBIDDEN);
                return;
            }
            session.setAttribute(keyInfo, valueInfo);
        } catch (NumberFormatException exception) {
            exception.printStackTrace();
            session.setAttribute(keyInfo, valueInfo);
            response.setStatus(SC_BAD_REQUEST);
        } catch (IOException exception) {
            LOGGER.info(exception.getMessage());
        }
    }

}
