// Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/http;

final http:Client securedEP = check new("https://localhost:9097",
    auth = {
        username: "ballerina",
        issuer: "wso2",
        audience: "vEwzbcasJVQm1jVYHUHCjhxZ4tYa",
        keyId: "NTAxZmMxNDMyZDg3MTU1ZGM0MzEzODJhZWI4NDNlZDU1OGFkNjFiMQ",
        customClaims: {
                "action": "add_order", "scp": "admin",  "scp1": "admin", "scp2": "admin", "scp3": "admin",
                "scp4": "admin", "scp5": "admin", "scp6": "admin", "scp7": "admin", "scp8": "admin",  "scp9": "admin",
                "scp10": "admin", "scp11": "admin", "scp12": "admin", "scp13": "admin", "scp14": "admin",
                "scp15": "admin", "scp16": "admin",  "scp17": "admin", "scp18": "admin", "scp19": "admin",
                "scp20": "admin", "scp21": "admin", "scp22": "admin", "scp23": "admin", "scp24": "admin",
                "scp25": "admin", "scp26": "admin", "scp27": "admin", "scp28": "admin", "scp29": "admin",
                "scp30": "admin", "scp31": "admin", "scp32": "admin",  "scp33": "admin", "scp34": "admin",
                "scp35": "admin", "scp36": "admin", "scp37": "admin", "scp38": "admin", "scp39": "admin",
                "scp40": "admin",  "scp41": "admin", "scp42": "admin", "scp43": "admin", "scp44": "admin",
                "scp45": "admin", "scp46": "admin", "scp47": "admin", "scp50": "admin",  "scp51": "admin",
                "scp52": "admin", "scp53": "admin", "scp54": "admin", "scp55": "admin", "scp56": "admin",
                "scp57": "admin", "scp60": "admin",  "scp61": "admin", "scp62": "admin", "scp63": "admin",
                "scp64": "admin", "scp65": "admin", "scp66": "admin", "scp67": "admin", "scp70": "admin",
                "scp71": "admin", "scp72": "admin", "scp73": "admin", "scp74": "admin", "scp75": "admin",
                "scp76": "admin", "scp77": "admin", "scp80": "admin",  "scp81": "admin", "scp82": "admin",
                "scp83": "admin", "scp84": "admin", "scp85": "admin", "scp86": "admin", "scp87": "admin",
                "scp90": "admin",  "scp91": "admin", "scp92": "admin", "scp93": "admin", "scp94": "admin",
                "scp95": "admin", "scp96": "admin", "scp97": "admin", "scp100": "admin",  "scp101": "admin",
                "scp102": "admin", "scp103": "admin", "scp104": "admin", "scp105": "admin", "scp106": "admin",
                "scp107": "admin", "scp110": "admin",  "scp111": "admin", "scp112": "admin", "scp113": "admin",
                "scp114": "admin", "scp115": "admin", "scp116": "admin", "scp117": "admin", "scp12p": "admin",
                "scp121": "admin", "scp122": "admin", "scp123": "admin", "scp124": "admin", "scp125": "admin",
                "scp126": "admin", "scp127": "admin"
        },
        expTime: 3600,
        signatureConfig: {
            config: {
                keyFile: "./resources/order_service/private.key"
            }
        }
    },
    secureSocket = {
        cert: "./resources/order_service/public.crt"
    }
);

isolated service /serv on new http:Listener(9098) {

    isolated resource function post .(@http:Payload json payload) returns json|error {
        return securedEP->post("/order", payload);
    }
}
