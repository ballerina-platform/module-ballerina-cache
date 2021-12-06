/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.stdlib.cache.compiler;

import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.UnaryExpressionNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.util.List;
import java.util.Optional;

/**
 * CacheConfigAnalyzer.
 */
public class CacheConfigValidator implements AnalysisTask<SyntaxNodeAnalysisContext> {

    @Override
    public void perform(SyntaxNodeAnalysisContext ctx) {
        List<Diagnostic> diagnostics = ctx.semanticModel().diagnostics();
        for (Diagnostic diagnostic : diagnostics) {
            if (diagnostic.diagnosticInfo().severity() == DiagnosticSeverity.ERROR) {
                return;
            }
        }
        Optional<Symbol> varSymOptional = ctx.semanticModel()
                .symbol(ctx.node());
        if (varSymOptional.isPresent()) {
            TypeSymbol typeSymbol = ((VariableSymbol) varSymOptional.get()).typeDescriptor();
            if (!isCacheConfigVariable(typeSymbol)) {
                return;
            }

            // Initiated with a record
            Optional<ExpressionNode> optionalInitializer;
            if ((ctx.node() instanceof VariableDeclarationNode)) {
                // Function level variables
                optionalInitializer = ((VariableDeclarationNode) ctx.node()).initializer();
            } else {
                // Module level variables
                optionalInitializer = ((ModuleVariableDeclarationNode) ctx.node()).initializer();
            }
            if (optionalInitializer.isEmpty()) {
                return;
            }
            ExpressionNode initializer = optionalInitializer.get();
            if (!(initializer instanceof MappingConstructorExpressionNode)) {
                return;
            }

            SeparatedNodeList<MappingFieldNode> fields =
                    ((MappingConstructorExpressionNode) initializer).fields();
            for (MappingFieldNode field : fields) {
                String name = ((SpecificFieldNode) field).fieldName().toString()
                        .trim().replaceAll(Constants.UNNECESSARY_CHARS_REGEX, "");
                ExpressionNode valueNode = ((SpecificFieldNode) field).valueExpr().get();
                switch (name) {
                    case Constants.CAPACITY:
                        int maxCapacity = Integer.parseInt(getTerminalNodeValue(valueNode));
                        if (maxCapacity <= 0) {
                            DiagnosticInfo diagnosticInfo = new DiagnosticInfo(
                                    DiagnosticsCodes.CACHE_101.getErrorCode(), DiagnosticsCodes.CACHE_101.getError(),
                                    DiagnosticsCodes.CACHE_101.getSeverity());

                            ctx.reportDiagnostic(
                                    DiagnosticFactory.createDiagnostic(diagnosticInfo, valueNode.location()));

                        }
                        break;
                    case Constants.EVICTION_FACTOR:
                        float evictionFactor = Float.parseFloat(getTerminalNodeValue(valueNode));
                        if (evictionFactor < 0 || evictionFactor >= 1) {
                            DiagnosticInfo diagnosticInfo = new DiagnosticInfo(
                                    DiagnosticsCodes.CACHE_102.getErrorCode(), DiagnosticsCodes.CACHE_102.getError(),
                                    DiagnosticsCodes.CACHE_102.getSeverity());
                            ctx.reportDiagnostic(
                                    DiagnosticFactory.createDiagnostic(diagnosticInfo, valueNode.location()));

                        }
                        break;
                    case Constants.DEFAULT_MAX_AGE:
                        float defaultMaxAge = Float.parseFloat(getTerminalNodeValue(valueNode));
                        if (defaultMaxAge != -1 && defaultMaxAge < 0) {
                            DiagnosticInfo diagnosticInfo = new DiagnosticInfo(
                                    DiagnosticsCodes.CACHE_103.getErrorCode(), DiagnosticsCodes.CACHE_103.getError(),
                                    DiagnosticsCodes.CACHE_103.getSeverity());
                            ctx.reportDiagnostic(
                                    DiagnosticFactory.createDiagnostic(diagnosticInfo, valueNode.location()));

                        }
                        break;
                    case Constants.CLEAN_UP_INTERVAL:
                        float cleanUpInterval = Float.parseFloat(getTerminalNodeValue(valueNode));
                        if (cleanUpInterval <= 0) {
                            DiagnosticInfo diagnosticInfo = new DiagnosticInfo(
                                    DiagnosticsCodes.CACHE_104.getErrorCode(), DiagnosticsCodes.CACHE_104.getError(),
                                    DiagnosticsCodes.CACHE_104.getSeverity());
                            ctx.reportDiagnostic(
                                    DiagnosticFactory.createDiagnostic(diagnosticInfo, valueNode.location()));

                        }
                        break;
                    default:
                        // Can ignore all other fields
                        continue;
                }
            }
        }
    }

    private String getTerminalNodeValue(Node valueNode) {
        String value;
        if (valueNode instanceof BasicLiteralNode) {
            value = ((BasicLiteralNode) valueNode).literalToken().text();
        } else {
            UnaryExpressionNode unaryExpressionNode = (UnaryExpressionNode) valueNode;
            value = unaryExpressionNode.unaryOperator() +
                    ((BasicLiteralNode) unaryExpressionNode.expression()).literalToken().text();
        }
        return value.replaceAll(Constants.UNNECESSARY_CHARS_REGEX, "");
    }

    private boolean isCacheConfigVariable(TypeSymbol type) {
        if (type.typeKind() == TypeDescKind.UNION) {
            return ((UnionTypeSymbol) type).memberTypeDescriptors().stream()
                    .filter(typeDescriptor -> typeDescriptor instanceof TypeReferenceTypeSymbol)
                    .map(typeReferenceTypeSymbol -> (TypeReferenceTypeSymbol) typeReferenceTypeSymbol)
                    .anyMatch(this::isCacheConfigVariable);
        }
        if (type.typeKind() == TypeDescKind.TYPE_REFERENCE) {
            return isCacheConfigVariable((TypeReferenceTypeSymbol) type);
        }
        return false;
    }

    private boolean isCacheConfigVariable(TypeReferenceTypeSymbol typeSymbol) {
        if (typeSymbol.typeDescriptor().typeKind() == TypeDescKind.RECORD) {
            ModuleSymbol moduleSymbol = typeSymbol.getModule().get();
            return Constants.CACHE.equals(moduleSymbol.getName().get()) &&
                    Constants.BALLERINA.equals(moduleSymbol.id().orgName())
                    && typeSymbol.definition().getName().get().equals(Constants.CACHE_CONFIG);
        }
        return false;
    }
}
