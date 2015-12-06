<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Verifying your account</title>
    </head>
    <body onload="getToken()">
		<div class="pulse-loader">
			Verifying…
		</div>
		<link rel="stylesheet" href="http://css-spinners.com/css/spinner/pulse.css" type="text/css">
		<script type="text/javascript">
			function getToken() {
				httpGetAsync('http://' + '${handsetURL}');
			}
			function httpGetAsync(theUrl) {
				var xmlHttp = new XMLHttpRequest();
				xmlHttp.onreadystatechange = function() { 
					if (xmlHttp.readyState == 4 && xmlHttp.status == 200) {
						postTokenCallback(xmlHttp.response);
					}
				}
				xmlHttp.open("GET", theUrl, true); 
				xmlHttp.send();
			}
			function post(path, params) {
				method = "post";
				var form = document.createElement("form");
				form.setAttribute("method", method);
				form.setAttribute("action", path);

				for (var key in params) {
					if (params.hasOwnProperty(key)) {
						var hiddenField = document.createElement("input");
						hiddenField.setAttribute("type", "hidden");
						hiddenField.setAttribute("name", key);
						hiddenField.setAttribute("value", params[key]);

						form.appendChild(hiddenField);
					 }
				}
				document.body.appendChild(form);
				form.submit();
			}
			function postTokenCallback(responseText) {
				post('/login/processToken',  {'token':responseText, 'sessionId':'${sessionId}'});
			}
		</script>
    </body>
</html>
