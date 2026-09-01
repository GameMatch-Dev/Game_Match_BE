param(
    [Parameter(Mandatory)] [string] $RequirementFile,
    [Parameter(Mandatory)] [string] $DiffFile,
    [Parameter(Mandatory)] [string] $ContextFile,
    [Parameter(Mandatory)] [string] $TestEvidenceFile,
    [Parameter(Mandatory)] [string] $RawOutputFile
)

$template = Get-Content (Join-Path $PSScriptRoot '..\docs\templates\claude-code-review-prompt.md') -Raw
$prompt = $template.Replace('{{REQUIREMENT}}', (Get-Content $RequirementFile -Raw))
$prompt = $prompt.Replace('{{DIFF}}', (Get-Content $DiffFile -Raw))
$prompt = $prompt.Replace('{{CONTEXT}}', (Get-Content $ContextFile -Raw))
$prompt = $prompt.Replace('{{TEST_EVIDENCE}}', (Get-Content $TestEvidenceFile -Raw))

if (-not (Get-Command claude -ErrorAction SilentlyContinue)) {
    throw 'Claude Code CLI was not found on PATH.'
}

$prompt | claude -p --input-format text --output-format json --permission-mode plan --tools '' --safe-mode --no-session-persistence | Set-Content $RawOutputFile
