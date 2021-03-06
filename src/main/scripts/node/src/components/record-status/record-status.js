import React from "react";
import Panel from "../layout/panel";
import ButtonWithModalWarning from "../modals/button-with-modal-warning";

class RecordStatus extends React.Component {

    componentWillReceiveProps(nextProps) {
        const { onFetchRecord } = this.props;

        if (nextProps.ipName !== this.props.ipName) {
            onFetchRecord(nextProps.ipName);
        }
    }

    componentDidMount() {
        const {record, ipName, onFetchRecord } = this.props;

        if (!record || record.ipName !== ipName) {
            onFetchRecord(ipName);
        }
    }


    render() {
        const { record, errorReport, processStatuses, repositories, errorStatuses, onReset } = this.props;
        if (!record) { return null; }

        const errorReportPanel = errorReport
            ? (
                <Panel title="Foutrapport">
                    <div className="row">
                        <label className="col-md-6">Statuscode</label>
                        <div className="col-md-26">
                            {errorReport.statusCode} - {errorStatuses[errorReport.statusCode] || ""}
                        </div>
                    </div>
                    <div className="row">
                        <label className="col-md-6">Melding</label>
                        <div className="col-md-26">
                            {errorReport.message}
                        </div>
                    </div>
                    <div className="row">
                        <label className="col-md-6">URL</label>
                        <div className="col-md-26">
                            <a href={errorReport.url} target="_blank">
                                {errorReport.url}
                            </a>
                        </div>
                    </div>
                    <div className="row">
                        <label className="col-md-6">Stacktrace</label>
                        <pre className="col-md-26">
                            {errorReport.stackTrace}
                        </pre>
                    </div>
                </Panel>
            )
            : null;

        const resetButton = processStatuses[record.state] === "failure" ? (
                <ButtonWithModalWarning
                    className="btn btn-default" label="Terugzetten in wachtrij"
                    onConfirm={(doClose) => {
                        onReset(record.ipName)
                        doClose();
                    }}>

                    Weet u zeker dat u deze publicatie wilt terugzetten in de wachtrij?
                </ButtonWithModalWarning>
            ) : null;

        return (
            <div>
                <Panel title="Overzicht IP">
                    <div className="row">
                        <label className="col-md-6">OAI/PMH identifier</label>
                        <div className="col-md-20">
                            <a target="_blank"
                               href={`http://oai.gharvester.dans.knaw.nl/?verb=GetRecord&metadataPrefix=nl_didl_norm&identifier=${encodeURIComponent(record.oaiIdentifier)}`}>
                                {record.oaiIdentifier}
                            </a>
                        </div>
                        <div className="col-md-6">
                            {resetButton}
                        </div>
                    </div>
                    <div className="row">
                        <label className="col-md-6">IP Name</label>
                        <div className="col-md-26">
                            {record.ipName}
                        </div>
                    </div>
                    <div className="row">
                        <label className="col-md-6">Status</label>
                        <div className="col-md-26">
                            {processStatuses[record.state]}
                        </div>
                    </div>
                    <div className="row">
                        <label className="col-md-6">Bron</label>
                        <div className="col-md-26">
                            {repositories.find(repo => repo.id === record.repositoryId).name}
                        </div>
                    </div>
                    <div className="row">
                        <label className="col-md-6">Aangemaakt</label>
                        <div className="col-md-26">
                            {record.tsCreate}
                        </div>
                    </div>
                    <div className="row">
                        <label className="col-md-6">Verwerkt</label>
                        <div className="col-md-26">
                            {record.tsProcessed || "-"}
                        </div>
                    </div>
                </Panel>
                {errorReportPanel}
            </div>
        );
    }
}

export default RecordStatus;
